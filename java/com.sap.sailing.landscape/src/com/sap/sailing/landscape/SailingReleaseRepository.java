package com.sap.sailing.landscape;

import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.impl.GithubReleasesRepository;

public interface SailingReleaseRepository extends ReleaseRepository {
    ReleaseRepository INSTANCE = new GithubReleasesRepository("https://github.com/SAP/sailing-analytics/releases/download/", /* main release name prefix */ "main");
}
