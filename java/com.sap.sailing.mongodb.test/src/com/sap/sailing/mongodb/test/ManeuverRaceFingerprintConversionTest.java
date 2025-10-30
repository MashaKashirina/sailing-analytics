package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.mongodb.MongoDBConfiguration;

public class ManeuverRaceFingerprintConversionTest extends OnlineTracTracBasedTest {
    DynamicTrackedRaceImpl trackedRace1;
    DynamicTrackedRaceImpl trackedRace2;
    private final MongoDBConfiguration dbConfiguration;

    public ManeuverRaceFingerprintConversionTest() throws MalformedURLException, URISyntaxException {
        super();
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
    }

    private MongoClient newMongo() throws UnknownHostException, MongoException {
        return MongoClients.create(dbConfiguration.getMongoClientURI());
    }

    @Override
    protected String getExpectedEventName() {
        return "Academy Tracking 2011";
    }

    @BeforeEach
    public void setUp() throws Exception {
        newMongo().getDatabase(dbConfiguration.getDatabaseName()).drop();
        super.setUp("event_20110505_SailingTea", // Semifinale
                /* raceId */ "01ea3604-02ef-11e1-9efc-406186cbf87c", /* liveUri */ null, /* storedUri */ null,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS, ReceiverType.SENSORDATA });
        trackedRace1 = getTrackedRace();
    }

    @Test
    public void testStoringToAndLoadingFromMongo() throws UnknownHostException, MongoException {
        ManeuverRaceFingerprintFactory factory = ManeuverRaceFingerprintFactory.INSTANCE;
        ManeuverRaceFingerprint fingerprint = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint.matches(trackedRace1));
        MongoClient myFirstMongo = newMongo();
        MongoDatabase firstDatabase = myFirstMongo.getDatabase(dbConfiguration.getDatabaseName());
        RaceIdentifier raceIdentifier = trackedRace1.getRaceIdentifier();
        //List<Competitor> competetors = trackedRace1.getCompetitor(null);
        final Map<Competitor, List<Maneuver>> maneuvers = new HashMap<>();
        for (Competitor competitor : getRace().getCompetitors()) {
            List<Maneuver> maneuversForCompetitor = (List<Maneuver>) trackedRace1.getManeuvers(competitor, true);
            maneuvers.put(competitor,maneuversForCompetitor);
        }
        //final Map<Competitor, List<Maneuver>> maneuvers = trackedRace1.getManeuvers(null, false);
        new MongoObjectFactoryImpl(firstDatabase).storeManeuvers(raceIdentifier, fingerprint, trackedRace1.getRace().getCourse(), maneuvers);
        DomainObjectFactory dF = PersistenceFactory.INSTANCE.getDomainObjectFactory(dbConfiguration.getService(), getDomainFactory().getBaseDomainFactory());
        Map<RaceIdentifier, ManeuverRaceFingerprint> fingerprintHashMap = dF.loadFingerprintsForManeuverHashes();
        ManeuverRaceFingerprint fingerprintAfterDB = fingerprintHashMap.get(trackedRace1.getRaceIdentifier());
        assertTrue(fingerprintAfterDB.matches(trackedRace1), "Original and de-serialized copy are equal");
        final Map<Competitor, List<Maneuver>> maneuversLoaded = dF.loadManeuvers(trackedRace1, trackedRace1.getRace().getCourse());
        assertEquals(maneuvers, maneuversLoaded);
    }
}
