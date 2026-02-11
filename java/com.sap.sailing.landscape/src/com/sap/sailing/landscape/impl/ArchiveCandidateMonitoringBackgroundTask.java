package com.sap.sailing.landscape.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.shiro.subject.Subject;

import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.NamedImpl;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.ReverseProxy;

/**
 * A stateful monitoring task that can be {@link #run run} to observe an {@code ARCHIVE} candidate process and wait for
 * it to be ready for comparing its contents with a production {@code ARCHIVE} instance. When run for the first time,
 * the task check the {@code /gwt/status} end point on {@code candidateHostname} to see whether it is already serving
 * requests. If not, it will re-schedule itself after some delay to check again until either the candidate becomes
 * healthy or a timeout is reached.
 * <p>
 * 
 * If the candidate was seen serving a {@code /gwt/status} request, this task changes state and now looks at the
 * contents of the status response. Four conditions must be fulfilled for the candidate to be considered ready for
 * comparison:
 * 
 * <ol>
 * <li>the overall status must be {@code available: true}.</li>
 * <li>the one-minute system load average must be below 2 (per cent)</li>
 * <li>the default foreground thread pool queue must contain less than 10 tasks</li>
 * <li>the default background thread pool queue must contain less than 10 tasks</li>
 * </ol>
 * 
 * When any of these conditions is not fulfilled, the task will re-schedule itself after some delay to check again until
 * either the candidate fulfills all conditions or a timeout is reached.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ArchiveCandidateMonitoringBackgroundTask implements Runnable {
    private interface Check extends Named {
        boolean runCheck() throws Exception;
        boolean hasTimedOut();
        Duration getDelayAfterFailure();
    }
    
    private abstract class AbstractCheck extends NamedImpl implements Check {
        private static final long serialVersionUID = -8809199091635882129L;
        private final TimePoint creationTime;
        private final Duration timeout;
        private final Duration delayAfterFailure;

        public AbstractCheck(String name, Duration timeout, Duration delayAfterFailure) {
            super(name);
            this.creationTime = TimePoint.now();
            this.timeout = timeout;
            this.delayAfterFailure = delayAfterFailure;
        }

        @Override
        public boolean hasTimedOut() {
            return creationTime.until(TimePoint.now()).compareTo(timeout) > 0;
        }
        
        @Override
        public Duration getDelayAfterFailure() {
            return delayAfterFailure;
        }
    }
    
    private static final Logger logger = Logger.getLogger(ArchiveCandidateMonitoringBackgroundTask.class.getName());

    private final static Duration DELAY_BETWEEN_CHECKS = Duration.ONE_MINUTE.times(5);
    private final static Duration LONG_TIMEOUT = Duration.ONE_DAY.times(3);
    private final static double MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE = 2.0;
    private final static int MAXIMUM_THREAD_POOL_QUEUE_SIZE = 10;
    private final static Optional<Duration> TIMEOUT_FIRST_CONTACT = Optional.of(Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().plus(Landscape.WAIT_FOR_HOST_TIMEOUT.get()));
    private final Subject subject;
    private final AwsLandscape<String> landscape;
    private final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet;
    private final ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> reverseProxyCluster;
    private final String optionalKeyName;
    private final byte[] privateKeyEncryptionPassphrase;
    private final ScheduledExecutorService executor;
    private final TimePoint firstRun;
    private final List<String> messagesToSendToProcessOwner;
    private Iterable<Check> checks;
    private Iterator<Check> checksIterator;
    private Check currentCheck;
    
    public ArchiveCandidateMonitoringBackgroundTask(Subject subject, AwsLandscape<String> landscape,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String candidateHostname,
            ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> reverseProxyCluster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, ScheduledExecutorService executor) {
        this.subject = subject;
        this.landscape = landscape;
        this.replicaSet = replicaSet;
        this.reverseProxyCluster = reverseProxyCluster;
        this.optionalKeyName = optionalKeyName;
        this.privateKeyEncryptionPassphrase = privateKeyEncryptionPassphrase;
        this.executor = executor;
        this.firstRun = TimePoint.now();
        this.messagesToSendToProcessOwner = new LinkedList<>();
        this.checks = Arrays.asList(
                new IsReady(),
                new HasLowEnoughSystemLoad(),
                new HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue(),
                new HasShortEnoughDefaultForegroundThreadPoolExecutorQueue(),
                new CompareServersWithRestAPI(),
                new CompareServersByLeaderboardGroups());
        this.checksIterator = this.checks.iterator();
        this.currentCheck = checksIterator.next();
    }

    @Override
    public void run() {
        try {
            if (currentCheck.runCheck()) {
                logger.info("Check "+currentCheck+" passed.");
                // the check passed; proceed to next check, if any
                currentCheck = checksIterator.hasNext() ? checksIterator.next() : null;
                if (currentCheck != null) {
                    logger.info("More checks to do; re-scheduling to run next check "+currentCheck);
                    // re-schedule this task to run next check immediately
                    executor.submit(this);
                } else {
                    logger.info("Done with all checks; candidate is ready for production.");
                    // all checks passed; candidate is ready for production; nothing more to do here
                }
            } else {
                rescheduleCurrentCheckAfterFailureOrTimeout();
            }
        } catch (Exception e) {
            logger.warning("Exception while running check " + currentCheck + " for candidate " + replicaSet.getMaster().getHost().getHostname() + ": " + e.getMessage());
        }
    }

    private void rescheduleCurrentCheckAfterFailureOrTimeout() {
        executor.schedule(this, currentCheck.getDelayAfterFailure().asMillis(), TimeUnit.MILLISECONDS);
    }

    private class IsReady extends AbstractCheck {
        private static final long serialVersionUID = -4265303532881568290L;

        private IsReady() {
            super("is ready", TIMEOUT_FIRST_CONTACT.get(), DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            return replicaSet.getMaster().isReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
        }
    }

    private class HasLowEnoughSystemLoad extends AbstractCheck {
        private static final long serialVersionUID = -7931266212387969287L;

        public HasLowEnoughSystemLoad() {
            super("has low enough system load", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            return replicaSet.getMaster().getLastMinuteSystemLoadAverage(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE;
        }
        
    }
    
    private class HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue extends AbstractCheck {
        private static final long serialVersionUID = 3482148861663152178L;

        public HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue() {
            super("has short enough default background thread pool executor queue", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            return replicaSet.getMaster().getDefaultBackgroundThreadPoolExecutorQueueSize(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
        }
    }

    private class HasShortEnoughDefaultForegroundThreadPoolExecutorQueue extends AbstractCheck {
        private static final long serialVersionUID = 5194383164577435150L;

        public HasShortEnoughDefaultForegroundThreadPoolExecutorQueue() {
            super("has short enough default foreground thread pool executor queue", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            return replicaSet.getMaster().getDefaultForegroundThreadPoolExecutorQueueSize(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
        }
    }
    
    private class CompareServersWithRestAPI extends AbstractCheck {
        private static final long serialVersionUID = -5271988056894947109L;

        public CompareServersWithRestAPI() {
            super("compare servers with REST API", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }


        @Override
        public boolean runCheck() throws Exception {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    private class CompareServersByLeaderboardGroups extends AbstractCheck {
        private static final long serialVersionUID = -5271988056894947109L;

        public CompareServersByLeaderboardGroups() {
            super("compare servers with Leaderboard Groups", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
