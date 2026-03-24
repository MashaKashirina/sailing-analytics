package com.sap.sailing.gwt.managementconsole.places.event.edit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.app.ManagementConsoleClientFactory;
import com.sap.sailing.gwt.managementconsole.places.AbstractManagementConsoleActivity;
import com.sap.sailing.gwt.managementconsole.places.event.overview.EventOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewPlace;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;

public class EditEventActivity extends AbstractManagementConsoleActivity<EditEventPlace> {

    private EditEventView editEventView;
    private EventDTO currentEvent;

    public EditEventActivity(final ManagementConsoleClientFactory clientFactory, final EditEventPlace place) {
        super(clientFactory, place);
    }

    @Override
    public void start(final AcceptsOneWidget container, final EventBus eventBus) {
        editEventView = new EditEventViewImpl();
        new EditEventViewPresenter(editEventView);
        container.setWidget(editEventView);

        // Load the event data
        loadEvent(getPlace().getEventId());
    }

    private void loadEvent(final UUID eventId) {
        getClientFactory().getEventService().getEvent(eventId, new AsyncCallback<EventDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify("Failed to load event", NotificationType.ERROR);
                getClientFactory().getPlaceController().goTo(new EventOverviewPlace());
            }

            @Override
            public void onSuccess(EventDTO result) {
                currentEvent = result;
                editEventView.populateForm(result);
            }
        });
    }

    private class EditEventViewPresenter implements EditEventView.Presenter {

        public EditEventViewPresenter(EditEventView editEventView) {
            editEventView.setPresenter(this);
        }

        @Override
        public void saveEvent(String name, String description, String venue, Date startDate, Date endDate,
                List<String> courseAreaNames, boolean isPublic) {

            // Update the event DTO with new values
            currentEvent.setName(name);
            currentEvent.setDescription(description);
            currentEvent.startDate = startDate;
            currentEvent.endDate = endDate;
            currentEvent.isPublic = isPublic;

            // Update venue
            VenueDTO venueDTO = currentEvent.getVenue();
            if (venueDTO == null) {
                venueDTO = new VenueDTO(venue);
                currentEvent.setVenue(venueDTO);
            } else {
                venueDTO.setName(venue);
            }

            // Update course areas
            List<CourseAreaDTO> courseAreas = new ArrayList<>();
            for (String caName : courseAreaNames) {
                courseAreas.add(new CourseAreaDTO(UUID.randomUUID(), caName));
            }
            venueDTO.setCourseAreas(courseAreas);

            getClientFactory().getEventService().updateEvent(currentEvent, getUpdateEventCallback());
        }

        private AsyncCallback<EventDTO> getUpdateEventCallback() {
            return new AsyncCallback<EventDTO>() {

                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify("Failed to save event: " + caught.getMessage(), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(EventDTO result) {
                    Notification.notify("Event saved successfully", NotificationType.SUCCESS);
                    getClientFactory().getPlaceController().goTo(new RegattaOverviewPlace(result.getId()));
                }
            };
        }

        @Override
        public void cancelEdit() {
            getClientFactory().getPlaceController().goTo(new EventOverviewPlace());
        }

    }

}
