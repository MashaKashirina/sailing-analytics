package com.sap.sailing.gwt.managementconsole.services;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.gwt.managementconsole.events.regatta.RegattaListResponseEvent;
import com.sap.sailing.gwt.managementconsole.services.factories.LeaderboardGroupFactory;
import com.sap.sailing.gwt.managementconsole.services.factories.RegattaFactory;
import com.sap.sailing.gwt.managementconsole.services.factories.SeriesWithRacesFactory;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupDialog.LeaderboardGroupDescriptor;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;

public class RegattaService {

    private static final Logger LOG = Logger.getLogger(RegattaService.class.getName());

    private final SailingServiceWriteAsync sailingService;
    private final ErrorReporter errorReporter;
    private final EventBus eventBus;

    private Map<String, RegattaDTO> regattaMap;

    public RegattaService(final SailingServiceWriteAsync sailingService,
            final ErrorReporter errorReporter,
            final EventBus eventBus) {
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.eventBus = eventBus;
        this.regattaMap = new HashMap<>();
    }

    public List<RegattaDTO> getRegattas() {
        return new ArrayList<>(regattaMap.values());
    }

    public void updateEvents(final List<RegattaDTO> regattas) {
        setRegattas(regattas);
    }

    public void requestRegattaList(final UUID eventId, final boolean forceRequestFromService) {
        if (forceRequestFromService || regattaMap.isEmpty()) {
            sailingService.getRegattasForEvent(eventId, new AsyncCallback<List<RegattaDTO>>() {
                @Override
                public void onFailure(final Throwable caught) {
                    LOG.severe("requestRegattaList :: Cannot load regattas!");
                    errorReporter.reportError("Error", "Cannot load regattas!");
                }

                @Override
                public void onSuccess(final List<RegattaDTO> regattas) {
                    LOG.info("requestRegattaList :: onSuccess");
                    setRegattas(regattas);     
                    eventBus.fireEvent(new RegattaListResponseEvent(regattas));
                }
           });
        } else {
            eventBus.fireEvent(new RegattaListResponseEvent(getRegattas()));
        }
    }

    private void setRegattas(final List<RegattaDTO> regattaList) {
        this.regattaMap = regattaList.stream().collect(toMap(regatta -> regatta.getName(), Function.identity()));
    }

    public void addRegatta(UUID eventId, String regattaName, String boatClassName, final RankingMetrics ranking, final Integer numberOfRaces, final ScoringSchemeType scoringSystem,
            AsyncCallback<RegattaDTO> callback) {  
        sailingService.getEventById(eventId, false, new AsyncCallback<EventDTO>() {
            @Override
            public final void onFailure(Throwable t) {
                callback.onFailure(t);
            }
            
            @Override
            public void onSuccess(EventDTO event) { 
                SeriesDTO series = SeriesWithRacesFactory.createSeriesWithRaces(regattaName, numberOfRaces, /* TODO oneAlwaysStaysOne */ false);                               
                RegattaDTO regattaDTO = RegattaFactory.createDefaultRegatta(regattaName, boatClassName, ranking, scoringSystem, series, event);
                createNewRegatta(event, regattaDTO, numberOfRaces, callback);
            }
        });
    }
    
    private void createNewRegatta(final EventDTO event, final RegattaDTO newRegatta, Integer racesCount, AsyncCallback<RegattaDTO> callback) {
        LinkedHashMap<String, SeriesCreationParametersDTO> seriesStructure = SeriesWithRacesFactory.createSeriesStructure(newRegatta.series.get(0));
        sailingService.createRegatta(newRegatta.getName(),
                newRegatta.boatClass == null ? null : newRegatta.boatClass.getName(),
                newRegatta.canBoatsOfCompetitorsChangePerRace, newRegatta.competitorRegistrationType,
                newRegatta.registrationLinkSecret, newRegatta.startDate, newRegatta.endDate,
                new RegattaCreationParametersDTO(seriesStructure), true, newRegatta.scoringScheme,
                Util.mapToArrayList(newRegatta.courseAreas, CourseAreaDTO::getId), newRegatta.buoyZoneRadiusInHullLengths,
                newRegatta.useStartTimeInference, newRegatta.controlTrackingFromStartAndFinishTimes,
                newRegatta.autoRestartTrackingUponCompetitorSetChange, newRegatta.rankingMetricType,
                new AsyncCallback<RegattaDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onSuccess(RegattaDTO regatta) {
                        // createDefaultRacesIfDefaultSeriesIsPresent(event, regatta);
                        createRegattaLeaderboard(event, newRegatta, callback);
                    }
                });
    }
    
    private void createRegattaLeaderboard(final EventDTO event, final RegattaDTO newRegatta, AsyncCallback<RegattaDTO> callback) {
        RegattaIdentifier regattaIdentifier = newRegatta.getRegattaIdentifier();
        sailingService.createRegattaLeaderboard((RegattaName)regattaIdentifier,/* displayName */ null, new int[] {},
                new AsyncCallback<StrippedLeaderboardDTO>() {
        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }

        @Override
        public void onSuccess(StrippedLeaderboardDTO newRegattaLeaderboard) {

            if (!newRegatta.courseAreas.isEmpty()) {
                addRegattaToRegattaLeaderboard(newRegattaLeaderboard, event, newRegatta, callback);
            }    
        }
    });
    }
    
    private void addRegattaToRegattaLeaderboard(StrippedLeaderboardDTO newRegattaLeaderboard, EventDTO event, final RegattaDTO newRegatta, 
            AsyncCallback<RegattaDTO> callback) {
        List<LeaderboardGroupDTO> leaderboardGroups = event.getLeaderboardGroups();
        if (leaderboardGroups == null || leaderboardGroups.isEmpty()) {
            createLeaderboardGroupAndAddRegattaToLeaderboard(newRegattaLeaderboard, event, newRegatta, callback);
        } else {
            addRegattaToRegattaLeaderboard(newRegattaLeaderboard, event.getLeaderboardGroups().get(0), event, newRegatta, callback);
        }
    }
    
    private void createLeaderboardGroupAndAddRegattaToLeaderboard(StrippedLeaderboardDTO newRegattaLeaderboard, EventDTO event, 
            final RegattaDTO newRegatta, AsyncCallback<RegattaDTO> callback) {
        LeaderboardGroupDescriptor newGroup = LeaderboardGroupFactory.createDefaultLeaderboardGroupDescriptor();
        sailingService.createLeaderboardGroup(newGroup.getName(), newGroup.getDescription(),
                newGroup.getDisplayName(), newGroup.isDisplayLeaderboardsInReverseOrder(),
                newGroup.getOverallLeaderboardDiscardThresholds(), newGroup.getOverallLeaderboardScoringSchemeType(), new MarkedAsyncCallback<LeaderboardGroupDTO>(
                        new AsyncCallback<LeaderboardGroupDTO>() {
                            @Override
                            public void onFailure(Throwable t) {
                                callback.onFailure(t);
                            }
                            @Override
                            public void onSuccess(LeaderboardGroupDTO newGroup) {
                                event.addLeaderboardGroup(newGroup);
                                updateEvent(newRegattaLeaderboard, event, newRegatta, callback);
                            }
                        }));
        
    }
    
    private void updateEvent(StrippedLeaderboardDTO newRegattaLeaderboard, EventDTO event, final RegattaDTO newRegatta, AsyncCallback<RegattaDTO> callback) {
        sailingService.updateEvent(event.id, event.getName(), event.getDescription(),
                event.startDate, event.endDate, event.getVenue(), event.isPublic,
                event.getLeaderboardGroupIds(), event.getOfficialWebsiteURL(), event.getBaseURL(),
                event.getSailorsInfoWebsiteURLs(), event.getImages(), event.getVideos(),
                event.getWindFinderReviewedSpotsCollectionIds(), new AsyncCallback<EventDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }   
                    @Override
                    public void onSuccess(EventDTO newEvent) {      
                        LeaderboardGroupDTO newGroup = newEvent.getLeaderboardGroups().get(0);
                        addRegattaToRegattaLeaderboard(newRegattaLeaderboard, newGroup, newEvent, newRegatta, callback);
                    }
                });
                    
    }

    public void getRegattas(AsyncCallback<List<RegattaDTO>> callback) {
        sailingService.getRegattas(new MarkedAsyncCallback<List<RegattaDTO>>(
                new AsyncCallback<List<RegattaDTO>>() {
                    @Override
                    public void onSuccess(List<RegattaDTO> result) {
                        callback.onSuccess(result);
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }
                }));
    }
    
    private void addRegattaToRegattaLeaderboard(StrippedLeaderboardDTO newRegattaLeaderboard, LeaderboardGroupDTO leaderboardGroup, EventDTO event,
            final RegattaDTO newRegatta, AsyncCallback<RegattaDTO> callback) {
        final List<String> leaderboardNames = new ArrayList<>();
        for (StrippedLeaderboardDTO leaderboard : leaderboardGroup.getLeaderboards()) {
            leaderboardNames.add(leaderboard.getName());
        }
        leaderboardNames.add(newRegattaLeaderboard.getName());
        sailingService.updateLeaderboardGroup(leaderboardGroup.getId(),
                leaderboardGroup.getName(), leaderboardGroup.getName(),
                leaderboardGroup.getDescription(), leaderboardGroup.getDisplayName(),
                leaderboardNames, leaderboardGroup.getOverallLeaderboardDiscardThresholds(),
                leaderboardGroup.getOverallLeaderboardScoringSchemeType(),
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        createDefaultRacesIfDefaultSeriesIsPresent(event, newRegatta, callback);     
                    }
               }));
    }
    
    private void createDefaultRacesIfDefaultSeriesIsPresent(final EventDTO event, final RegattaDTO newRegatta, AsyncCallback<RegattaDTO> callback) {
        for (final SeriesDTO series: newRegatta.series) {
            if (series.getName().equals(LeaderboardNameConstants.DEFAULT_SERIES_NAME) && !series.getRaceColumns().isEmpty()) {
                final List<Pair<String, Integer>> raceColumnNamesToAddWithInsertIndex = new ArrayList<>();
                for (RaceColumnDTO newRaceColumn : series.getRaceColumns()) {
                    // We could use an index counter here because we're assuming that we're creating
                    // races starting at index 0. However, to make things concurrency-safe, we have to
                    // assume that while the regatta already exists on the server, some other activity
                    // may already have started to create races for it. Better safe than sorry, append
                    // at the end, using -1 as "insertIndex."
                    raceColumnNamesToAddWithInsertIndex.add(new Pair<>(newRaceColumn.getName(), -1));
                }
        
                sailingService.addRaceColumnsToSeries(newRegatta.getRegattaIdentifier(), series.getName(),
                        raceColumnNamesToAddWithInsertIndex, new AsyncCallback<List<RaceColumnInSeriesDTO>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onSuccess(List<RaceColumnInSeriesDTO> raceColumns) {
                        requestRegattaList(event.id, true);
                        callback.onSuccess(newRegatta);
                    }
                });     
            }
        }    
    } 

}
