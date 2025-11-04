package com.sap.sailing.gwt.home.mobile.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.desktop.app.ApplicationTopLevelView;
import com.sap.sailing.gwt.home.mobile.partials.footer.Footer;
import com.sap.sailing.gwt.home.mobile.partials.header.Header;
import com.sap.sailing.gwt.home.shared.app.HasLocationTitle;
import com.sap.sailing.gwt.home.shared.app.ResettableNavigationPathDisplay;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DefaultErrorReporter;
import com.sap.sse.gwt.client.ErrorReporter;

/**
 * This is the top-level view of the application. Every time another presenter wants to reveal itself,
 * {@link MobileApplicationView} will add its content of the target inside the {@code mainContantPanel}.
 */
public class MobileApplicationView extends Composite implements ApplicationTopLevelView<ResettableNavigationPathDisplay> {
    interface MyBinder extends UiBinder<Widget, MobileApplicationView> {
    }

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    private static ErrorReporter errorReporter = new DefaultErrorReporter<StringMessages>(StringMessages.INSTANCE);

    @UiField(provided=true)
    Header headerPanel;

    @UiField(provided=true)
    Footer footerPanel;

    @UiField
    SimplePanel mainContentPanel;
    
    @UiField
    SimplePanel subHeaderPanel;

    public MobileApplicationView(MobilePlacesNavigator placeNavigator, EventBus eventBus) {
        headerPanel = new Header(placeNavigator, eventBus);
        footerPanel = new Footer(placeNavigator);
        initWidget(uiBinder.createAndBindUi(this));
        eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                Place newPlace = event.getNewPlace();
                if (newPlace instanceof HasLocationTitle) {
                    HasLocationTitle hasLocationTitle = (HasLocationTitle) newPlace;
                    headerPanel.setLocationTitle(hasLocationTitle.getLocationTitle());
                }
            }
        });
    }

    @Override
    public AcceptsOneWidget getContent() {
        return mainContentPanel;
    }
    
    @Override
    public void showLoading(boolean visible) {
    }
    
    @Override
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    @Override
    public ResettableNavigationPathDisplay getNavigationPathDisplay() {
        return headerPanel.getNavigationPathDisplay();
    }
    
    public void setSubHeaderContent(IsWidget content) {
        subHeaderPanel.setWidget(content);
    }
}
