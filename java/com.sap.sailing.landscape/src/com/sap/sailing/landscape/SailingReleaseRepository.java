package com.sap.sailing.landscape;

import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.impl.FolderBasedReleaseRepositoryImpl;

public interface SailingReleaseRepository extends ReleaseRepository {
    // TODO bug6203: avoid rate limits by temporarily using the old folder-based releases repo again:
    ReleaseRepository INSTANCE = new FolderBasedReleaseRepositoryImpl("https://releases.sapsailing.com", /* master release name prefix */ "main");

    default void m() { int TODO_UseGithubReleasesRepositoryAgain; }
//    ReleaseRepository INSTANCE = new GithubReleasesRepository(
//            "SAP",               // owner
//            "sailing-analytics", // repo name
//            "main");             // main release name prefix
}
