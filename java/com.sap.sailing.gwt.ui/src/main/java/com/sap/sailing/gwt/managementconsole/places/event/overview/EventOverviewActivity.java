package com.sap.sailing.gwt.managementconsole.places.event.overview;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.common.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.shared.partials.header.HeaderConstants;
import com.sap.sailing.gwt.managementconsole.app.ManagementConsoleClientFactory;
import com.sap.sailing.gwt.managementconsole.events.EventListResponseEvent;
import com.sap.sailing.gwt.managementconsole.partials.contextmenu.ContextMenu;
import com.sap.sailing.gwt.managementconsole.places.AbstractManagementConsoleActivity;
import com.sap.sailing.gwt.managementconsole.places.event.create.CreateEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.edit.EditEventPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewPlace;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.formfactor.DeviceDetector;

public class EventOverviewActivity extends AbstractManagementConsoleActivity<EventOverviewPlace>
        implements EventOverviewView.Presenter {

    private final EventOverviewView view;
    private final StringMessages i18n = StringMessages.INSTANCE;

    public EventOverviewActivity(final ManagementConsoleClientFactory clientFactory, final EventOverviewPlace place) {
        super(clientFactory, place);
        this.view = getClientFactory().getViewFactory().getEventOverviewView();
        this.view.setPresenter(this);
    }

    @Override
    public void start(final AcceptsOneWidget container, final EventBus eventBus) {
        eventBus.addHandler(EventListResponseEvent.TYPE, events -> {
            view.renderEvents(events);
            container.setWidget(view);
        });
        getClientFactory().getEventService().requestEventList(/* forceRequestFromService */ false);
    }

    @Override
    public void reloadEventList() {
        getClientFactory().getEventService().requestEventList(/* forceRequestFromService */ true);
    }

    @Override
    public void requestContextMenu(final EventMetadataDTO event) {
        view.showContextMenu(event);
    }

    @Override
    public void navigateToEvent(final EventMetadataDTO event) {
        getClientFactory().getPlaceController().goTo(new RegattaOverviewPlace(event.getId()));
    }

    @Override
    public void navigateToCreateEvent() {
        getClientFactory().getPlaceController().goTo(new CreateEventPlace());
    }

    @Override
    public void editEvent(final EventMetadataDTO event) {
        getClientFactory().getPlaceController().goTo(new EditEventPlace(event.getId()));
    }

    @Override
    public void advancedSettings(ManagementConsoleResources app_res, final EventMetadataDTO event) {
        if (DeviceDetector.isMobile()) {
            ContextMenu confirmSwitch = new ContextMenu();
            confirmSwitch.setTitle(i18n.redirectToWebsiteNotOptimizedForMobileDevices());
            confirmSwitch.setHeaderWidget(new Label(i18n.redirectToWebsiteNotOptimizedForMobileDevices()));
            confirmSwitch.addItem(i18n.confirm(), app_res.icons().iconSettings(),
                    e -> jumpToAdminConsole(event.getDisplayName()));
            confirmSwitch.addItem(i18n.cancel(), app_res.icons().iconClose(),
                    e -> GWT.log("Jump to AdminConsole canceled."));
            // set the confirm option as primary action
            confirmSwitch.setPrimaryItemIndex(0);
            confirmSwitch.show();
        } else {
            jumpToAdminConsole(event.getDisplayName());
        }
    }

    private void jumpToAdminConsole(String eventName) {
        Window.Location.replace(HeaderConstants.ADMIN_CONSOLE_PATH + "#EventsPlace:filterAndSelect=" + eventName);
    }

    @Override
    public void deleteEvent(final EventMetadataDTO event) {
        Notification.notify("Deleting event: " + event.getId(), NotificationType.WARNING);
    }
}
