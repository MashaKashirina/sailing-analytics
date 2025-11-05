package com.sap.sse.landscape.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class GithubReleasesRepository extends AbstractReleaseRepository implements ReleaseRepository {
    private final static Logger logger = Logger.getLogger(GithubReleasesRepository.class.getName());
    
    public GithubReleasesRepository(String repositoryBase, String mainReleaseNamePrefix) {
        super(repositoryBase, mainReleaseNamePrefix);
    }

    @Override
    protected Iterable<Release> getAvailableReleases() {
        URLConnection connection;
        try {
            connection = HttpUrlConnectionHelper.redirectConnection(new URL(getRepositoryBase()));
            final InputStream index = (InputStream) connection.getContent();
            final JSONArray releasesJson = (JSONArray) new JSONParser().parse(new InputStreamReader(index));
            // TODO
            return null;
        } catch (IOException | ParseException e) {
            logger.warning("Exception trying to find releases: "+e.getMessage());
        }
        return null; // TODO 
    }

}
