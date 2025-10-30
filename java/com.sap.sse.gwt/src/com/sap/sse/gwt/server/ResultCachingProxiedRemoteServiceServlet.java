package com.sap.sse.gwt.server;

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.sap.sse.common.CacheableRPCResult;

/**
 * {@link ProxiedRemoteServiceServlet} that automatically caches serialized RPC results for result objects that
 * implement {@link CacheableRPCResult}.
 */
public class ResultCachingProxiedRemoteServiceServlet extends DelegatingProxiedRemoteServiceServlet {
    private static final Logger logger = Logger.getLogger(ResultCachingProxiedRemoteServiceServlet.class.getName());
    
    private static final long serialVersionUID = -4245484615349695611L;
    
    private Map<CacheKey, String> resultCache = new ConcurrentHashMap<>();
    
    /**
     * Holds the weak references to {@link CacheableRPCResult} objects that have been garbage-collected
     * and hence dereferenced. This will set the {@link WeakReference}'s {@link WeakReference#get() referent}
     * to {@code null} but leave the reference in the cache. Upon cache access we will purge cache
     * entries whose {@link CacheableRPCResult} has appeared in this queue.
     */
    final ReferenceQueue<CacheableRPCResult> dereferencedObjectsQueue;
    
    private final AtomicLong callCount;
    private final AtomicLong recalcCount;
    public class RPCSerializedResultCacheMXBeanImpl implements RPCSerializedResultCacheMXBean {
        @Override
        public long getCallCount() {
            return callCount.get();
        }
        
        @Override
        public long getRecalcCount() {
            return recalcCount.get();
        }
        
        @Override
        public long getNumberOfCachedResults() {
            return resultCache.size();
        }
        
        @Override
        public long getTotalCacheSize() {
            long result = 0;
            for (final String cachedString : resultCache.values()) {
                result += cachedString.length();
            }
            return result;
        }
    }
    
    public ResultCachingProxiedRemoteServiceServlet() {
        callCount = new AtomicLong();
        recalcCount = new AtomicLong();
        dereferencedObjectsQueue = new ReferenceQueue<>();
        // Add an MBean for the service to the JMX bean server:
        final RPCSerializedResultCacheMXBean mbean = new RPCSerializedResultCacheMXBeanImpl();
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            final ObjectName mBeanName = new ObjectName("com.sap.sse:type=GWTRPCSerializedResultCache_"+
                    getClass().getSimpleName()+"_"+(toString().substring(toString().indexOf('@')+1)));
            mbs.registerMBean(mbean, mBeanName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't register MXBean for result-caching proxy "+this, e);
        }
    }
    
    @Override
    protected String encodeResponseForSuccess(Method serviceMethod, SerializationPolicy serializationPolicy, int flags,
            Object result) throws SerializationException {
        purgeCache();
        if (result instanceof CacheableRPCResult) {
            final CacheableRPCResult cacheableResult = (CacheableRPCResult) result;
            try {
                callCount.incrementAndGet();
                return resultCache.computeIfAbsent(new CacheKey(serializationPolicy, cacheableResult, dereferencedObjectsQueue),
                        key -> {
                            recalcCount.incrementAndGet();
                            try {
                                return super.encodeResponseForSuccess(serviceMethod, serializationPolicy, flags, result);
                            } catch (SerializationException serializationException) {
                                throw new TemporaryWrapperException(serializationException);
                            }
                        });
            } catch (TemporaryWrapperException e) {
                throw e.serializationException;
            }
        }
        return super.encodeResponseForSuccess(serviceMethod, serializationPolicy, flags, result);
    }
    
    /**
     * For all references enqueued in {@link #dereferencedObjectsQueue}, removes their key from the {@link #resultCache}.
     */
    private void purgeCache() {
        Reference<? extends CacheableRPCResult> clearedReference;
        while ((clearedReference = dereferencedObjectsQueue.poll()) != null) {
            resultCache.remove(clearedReference);
        }
    }

    private class TemporaryWrapperException extends RuntimeException {
        private static final long serialVersionUID = 4720608117776979002L;
        private final SerializationException serializationException;

        public TemporaryWrapperException(SerializationException serializationException) {
            this.serializationException = serializationException;
        }
    }
}
