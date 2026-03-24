package com.sap.sailing.gwt.managementconsole.places.regatta.overview.partials;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.BoatClassImageResolver;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewResources;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewView.Presenter;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;

public class RegattaCard extends Composite {

    interface RegattaCardUiBinder extends UiBinder<Widget, RegattaCard> {
    }

    private static RegattaCardUiBinder uiBinder = GWT.create(RegattaCardUiBinder.class);

    @UiField
    RegattaOverviewResources local_res;
    @UiField
    Element card, container, title, details;
    @UiField
    AnchorElement logoUi;
    @UiField
    SpanElement racesUi;

    private final RegattaDTO regatta;
    private final Presenter presenter;

    public RegattaCard(final RegattaDTO regatta, final Presenter presenter) {
        initWidget(uiBinder.createAndBindUi(this));
        this.regatta = regatta;
        this.presenter = presenter;
        local_res.style().ensureInjected();
        String boatClass = regatta.boatClass.getName();
        ImageResource logo = BoatClassImageResolver.getBoatClassIconResource(boatClass);
        logoUi.getStyle().setBackgroundImage("url('" + logo.getSafeUri().asString() + "')");
        String date = regatta.getStartDate() == null ? "" :
            DateAndTimeFormatterUtil.longDateFormatter.render(regatta.getStartDate());
        racesUi.setInnerText(getNumberOfRaces() + " races");
        this.title.setInnerSafeHtml(SafeHtmlUtils.fromString(regatta.getName()));
        this.details.setInnerSafeHtml(SafeHtmlUtils.fromString(date));
    }

    @UiHandler("contextMenu")
    void onSettingsIconClick(final ClickEvent regatta) {
        presenter.navigateToRegatta(this.regatta);
    }

    private int getNumberOfRaces() {
        if (regatta.series.isEmpty()) {
            return 0;
        } else {
            return regatta.series.get(0).getRaceColumns().size();
        }  
    }
}
