package com.sap.sailing.gwt.home.desktop.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.desktop.partials.footer.Footer;
import com.sap.sailing.gwt.home.desktop.partials.header.Header;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DefaultErrorReporter;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.breadcrumb.BreadcrumbPane;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuView;

/**
 * This is the top-level view of the application. Every time another presenter wants to reveal itself,
 * {@link TabletAndDesktopApplicationView} will add its content of the target inside the {@code mainContantPanel}.
 */
public class TabletAndDesktopApplicationView extends Composite implements DesktopApplicationTopLevelView {
    interface TabletAndDesktopApplicationViewUiBinder extends UiBinder<Widget, TabletAndDesktopApplicationView> {
    }

    private static TabletAndDesktopApplicationViewUiBinder uiBinder = GWT.create(TabletAndDesktopApplicationViewUiBinder.class);

    private static ErrorReporter errorReporter = new DefaultErrorReporter<StringMessages>(StringMessages.INSTANCE);

    @UiField(provided=true)
    Header headerPanel;

    @UiField(provided=true)
    Footer footerPanel;

    @UiField
    SimplePanel mainContentPanel;
    
    @UiField
    BreadcrumbPane breadcrumbsUi;
    
    @UiField
    DivElement breadcrumbWrapperUi;
    
    private final DesktopResettableNavigationPathDisplay navigationPathDisplay;

    public TabletAndDesktopApplicationView(DesktopPlacesNavigator navigator, EventBus eventBus) {
        headerPanel = new Header(navigator, eventBus);
        footerPanel = new Footer(navigator, eventBus);
        initWidget(uiBinder.createAndBindUi(this));
        navigationPathDisplay = new BreadcrumbNavigationPathDisplay();
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
    public AuthenticationMenuView getAuthenticationMenuView() {
        return headerPanel.getAuthenticationMenuView();
    }
    
    @Override
    public DesktopResettableNavigationPathDisplay getNavigationPathDisplay() {
        return navigationPathDisplay;
    }
    
    private class BreadcrumbNavigationPathDisplay implements DesktopResettableNavigationPathDisplay {
        @Override
        public void setWithHeader(boolean withHeader) {
            if(withHeader) {
                breadcrumbWrapperUi.getStyle().setBackgroundColor("#f2f2f2");
            } else {
                breadcrumbWrapperUi.getStyle().clearBackgroundColor();;
            }
        }
        
        @Override
        public void showNavigationPath(NavigationItem... navigationPath) {
            breadcrumbsUi.clear();
            for (NavigationItem navigationPathDisplay : navigationPath) {
                breadcrumbsUi.addBreadcrumbItem(navigationPathDisplay.getDisplayName(),
                        navigationPathDisplay.getTargetUrl(), navigationPathDisplay);
            }
            breadcrumbWrapperUi.getStyle().clearDisplay();
        }

        @Override
        public void reset() {
            breadcrumbsUi.clear();
            breadcrumbWrapperUi.getStyle().setDisplay(Display.NONE);
            breadcrumbWrapperUi.getStyle().clearBackgroundColor();
        }
        
    }

}
