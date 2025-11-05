package com.sap.sse.landscape.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Assumes a public GitHub repository where releases can be freely downloaded from
 * <code>https://github.com/{owner}/{repo}/releases/download/{release-name}</code>. The GitHub
 * {@code /releases} end point delivers the releases in descending chronological order, so
 * newest releases first. With this, we can cache old results and try to get along with the
 * harsh rate limit of only 60 requests per hour when used without authentication.
 * 
 * @author Axel Uhl (d043530)
 */
public class GithubReleasesRepository extends AbstractReleaseRepository implements ReleaseRepository {
    private final static Logger logger = Logger.getLogger(GithubReleasesRepository.class.getName());
    private static final SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    private final static String GITHUB_API_BASE_URL = "https://api.github.com";
    private final static String GITHUB_BASE_URL = "https://github.com";
    private final String owner;
    private final String repositoryName;
    private final TreeMap<TimePoint, Release> releasesByPublishingTimePoint;
    
    public GithubReleasesRepository(String owner, String repositoryName, String defaultReleaseNamePrefix) {
        super(defaultReleaseNamePrefix);
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.releasesByPublishingTimePoint = new TreeMap<>();
    }
    
    private String getRepositoryPath() {
        return owner+"/"+repositoryName;
    }
    
    private String getReleasesURL() {
        return GITHUB_API_BASE_URL+"/repos/"+getRepositoryPath()+"/releases?per_page=100";
    }

    @Override
    public Release getRelease(String releaseName) {
        return new GithubRelease(releaseName, GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+releaseName+Release.ARCHIVE_EXTENSION,
                GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+Release.RELEASE_NOTES_FILE_NAME);
    }
    
    /**
     * Always fetches the first page from the {@code /releases} end point and starts constructing releases, until a
     * publishing time point overlap with {@link GithubReleasesRepository#releasesByPublishingTimePoint} is found. Then
     * we know we can continue to enumerate the remaining releases from that cache.
     * <p>
     * 
     * All releases found by loading a page are added to the
     * {@link GithubReleasesRepository#releasesByPublishingTimePoint} cache.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class ReleaseIterator implements Iterator<Release> {
        private String nextPageURL;
        private Iterator<Pair<TimePoint, GithubRelease>> publishingTimePointsAndReleasesFromCurrentPageIterator;
        
        private ReleaseIterator() throws MalformedURLException, IOException, ParseException {
            nextPageURL = getReleasesURL();
            loadNextPage();
        }
        
        private void loadNextPage() throws MalformedURLException, IOException, ParseException {
            final List<Pair<TimePoint, GithubRelease>> result = new LinkedList<>();
            final URLConnection connection = HttpUrlConnectionHelper.redirectConnection(new URL(nextPageURL));
            final InputStream index = (InputStream) connection.getContent();
            final String linkHeader = connection.getHeaderField("link");
            nextPageURL = getNextPageURL(linkHeader);
            final JSONArray releasesJson = (JSONArray) new JSONParser().parse(new InputStreamReader(index));
            for (final Object releaseObject : releasesJson) {
                final Pair<TimePoint, GithubRelease> publishedAtAndRelease = getPublishedAtAndReleaseFromJson((JSONObject) releaseObject);
                releasesByPublishingTimePoint.put(publishedAtAndRelease.getA(), publishedAtAndRelease.getB());
                result.add(publishedAtAndRelease);
            }
            publishingTimePointsAndReleasesFromCurrentPageIterator = result.iterator();
        }

        @Override
        public boolean hasNext() {
            return publishingTimePointsAndReleasesFromCurrentPageIterator.hasNext() || nextPageURL != null;
        }

        @Override
        public Release next() {
            if (!publishingTimePointsAndReleasesFromCurrentPageIterator.hasNext()) {
                try {
                    // FIXME bug6173: only load next page if we have to... we may already have created an overlap with the cache from releasesByPublishingTimePoint
                    loadNextPage();
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            return publishingTimePointsAndReleasesFromCurrentPageIterator.next().getB();
        }
    }
    
    @Override
    public Iterator<Release> iterator() {
        try {
            return new ReleaseIterator();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Release getLatestRelease(String releaseNamePrefix) {
        // TODO Auto-generated method stub
        return super.getLatestRelease(releaseNamePrefix);
    }

    /**
     * Enumerating all releases of the GitHub repo is possible but goes against the harsh rate limit when used without
     * an access token (currently only 60 requests per hour), so should ideally be avoided altogether. And if it is ever called,
     * we will cache the results, so that for later requests we typically need to query only a single page, delivering the latest
     * additions, if any.
     */
    private Iterable<Release> getAvailableReleases() {
        final List<Release> result = new LinkedList<>();
        try {
            String nextPageURL = getReleasesURL();
            do {
                final URLConnection connection = HttpUrlConnectionHelper.redirectConnection(new URL(nextPageURL));
                final InputStream index = (InputStream) connection.getContent();
                final String linkHeader = connection.getHeaderField("link");
                final JSONArray releasesJson = (JSONArray) new JSONParser().parse(new InputStreamReader(index));
                addAllReleasesTo(releasesJson, result);
                nextPageURL = getNextPageURL(linkHeader);
            } while (nextPageURL != null);
        } catch (IOException | ParseException e) {
            logger.warning("Exception trying to find releases: "+e.getMessage());
        }
        return result;
    }

    private void addAllReleasesTo(JSONArray releasesJson, List<Release> result) {
        for (final Object releaseObject : releasesJson) {
            final Pair<TimePoint, GithubRelease> publishedAtAndRelease = getPublishedAtAndReleaseFromJson((JSONObject) releaseObject);
            result.add(publishedAtAndRelease.getB());
            releasesByPublishingTimePoint.put(publishedAtAndRelease.getA(), publishedAtAndRelease.getB());
        }
    }
    
    private Pair<TimePoint, GithubRelease> getPublishedAtAndReleaseFromJson(JSONObject releaseJson) {
        final String name = releaseJson.get("name").toString();
        final String publishedAtISO = releaseJson.get("published_at").toString();
        TimePoint publishedAt;
        try {
            publishedAt = TimePoint.of(isoDateTimeFormat.parse(publishedAtISO));
        } catch (java.text.ParseException e) {
            logger.warning("Couldn't read published_at time stamp for release "+name+": "+publishedAtISO);
            throw new RuntimeException(e);
        }
        String archiveDownloadURL = null;
        String releaseNotesURL = null;
        for (final Object archiveAsset : (JSONArray) releaseJson.get("assets")) {
            final JSONObject archiveAssetJson = (JSONObject) archiveAsset;
            if (archiveAssetJson.get("content_type").equals("application/x-tar")) {
                archiveDownloadURL = archiveAssetJson.get("browser_download_url").toString();
            } else if (archiveAssetJson.get("name").equals(Release.RELEASE_NOTES_FILE_NAME)) {
                releaseNotesURL = archiveAssetJson.get("browser_download_url").toString();
            }
        }
        final GithubRelease release = new GithubRelease(name, archiveDownloadURL, releaseNotesURL);
        return new Pair<>(publishedAt, release);
    }

    private static final Pattern nextPagePattern = Pattern.compile(".*<([^<]*)>; rel=\"next\".*");
    String getNextPageURL(String linkHeader) {
        final String result;
        final Matcher m = nextPagePattern.matcher(linkHeader);
        if (m.matches()) {
            result = m.group(1);
        } else {
            result = null;
        }
        return result;
    }
}
