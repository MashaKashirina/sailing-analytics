package com.sap.sse.branding.sap;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import com.sap.sse.branding.shared.BrandingConfiguration;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class SAPBrandingConfiguration implements BrandingConfiguration {
    public static final String ID = "SAP";
    private String defaultBrandingLogoURL;
    private String greyTransparentLogoURL;
    private final ResourceBundleStringMessages sailingServerStringMessages;
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/SAPBrandingStringMessages";

    public SAPBrandingConfiguration() {
        sailingServerStringMessages = ResourceBundleStringMessages.create(STRING_MESSAGES_BASE_NAME, getClass().getClassLoader(),
                StandardCharsets.UTF_8.name());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDefaultBrandingLogoURL(Optional<String> locale) {
        return defaultBrandingLogoURL;
    }

    @Override
    public String getGreyTransparentLogoURL(Optional<String> locale) {
        return greyTransparentLogoURL;
    }

    @Override
    public String getBrandTitle(Optional<String> locale) {
        return "SAP";
    }
    
    @Override
    public String getSolutionsInSailingImageURL() {
        return "/sap-branding/images/solutions-sap-in-sailing.jpg";
    }

    @Override
    public String getSoutionsInSailingTrimmedImageURL() {
        return "/sap-branding/images/solutions-sap-trimmed.png";
    }

    @Override
    public String getSailingRaceManagerAppImageURL() {
        return "/sap-branding/images/solutions-sap-sailing-race-manager.png";
    }

    @Override
    public String getSailingRaceManagerAppTrimmedImageURL() {
        return "/sap-branding/images/solutions-race.png";
    }

    @Override
    public String getSailInSightAppImageURL() {
        return "/sap-branding/images/solutions-sap-sailing-insight.png";
    }
    
    @Override
    public String getSailingSimulatorImageURL() {
        return "/sap-branding/images/solutions-simulator.png";
    }

    @Override
    public String getSailingSimulatorTrimmedImageURL() {
        return "/sap-branding/images/solutions-simulator-trimmed.png";
    }

    @Override
    public String getBuoyPingerAppImageURL() {
        return "/sap-branding/images/solutions-sap-sailing-buoy-pinger.png";
    }

    @Override
    public String getSailingAnalyticsImageURL() {
        return "/sap-branding/images/solutions-sap.png";
    }

    @Override
    public String getSailingAnalyticsReadMoreText(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sailingAnalyticsReadMore");
    }
    
    @Override
    public String getSailingAnalyticsSailing(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sailingAnalyticsSailing");
    }

    @Override
    public String getFooterCopyright() {
        return "@ 2011-2025 SAP Sailing Analytics";
    }

    @Override
    public String getFooterLegalLink() {
        return "/gwt/Home.html#/imprint/:";
    }

    @Override
    public String getFooterPrivacyLink() {
        return "https://www.sap.com/about/legal/privacy.html?campaigncode=CRM-XH21-OSP-Sailing";
    }

    @Override
    public String getFooterJobsLink() {
        return "https://jobs.sapsailing.com";
    }

    @Override
    public String getFooterSupportLink() {
        return "https://support.sapsailing.com";
    }

    @Override
    public String getFooterWhatsNewLink() {
        return "/gwt/Home.html#WhatsNewPlace:navigationTab=SailingAnalytics";
    }

    @Override
    public String getSportsOn(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sportsOn");
    }

    @Override
    public String getFollowSports(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "followSports");
    }

    @Override
    public String getFacebookLink() {
        return "https://www.facebook.com/SAP";
    }

    @Override
    public String getxLink() {
        return "https://x.com/sap";
    }

    @Override
    public String getInstagramLink() {
        return "https://www.instagram.com/sap/";
    }
    @Override
    public String getWelcomeToSailingAnalytics(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "welcomeToSailingAnalytics");
    }

    @Override
    public String getWelcomeToSailingAnalyticsBody(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "welcomeToSailingAnalyticsBody");
    }
    
    @Override
    public String getMoreLoginInformationNotificationsURL() {
        return "/sap-branding/images/notifications.png";
    }
    
    @Override
    public String getMoreLoginInformationSettingsURL() {
        return "/sap-branding/images/settings.png";
    }
    
    @Override
    public String getMoreLoginInformationSailorProfilesURL() {
        return " /sap-branding/images/sailorprofiles.png";
    }
    
    @Override
    public String getMoreLoginInformationSimulatorURL() {
        return "/sap-branding/images/simulator.png";
    }
    @Override
    public String getInSailingContent(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l -> Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "inSailingContent");
    }
}
