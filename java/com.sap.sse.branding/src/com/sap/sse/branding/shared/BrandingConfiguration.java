package com.sap.sse.branding.shared;

import java.util.Optional;

public interface BrandingConfiguration {
    /**
     * The ID by which to find or set this configuration in {@code BrandingConfigurationService#setActiveBrandingConfigurationById(String)}
     */
    String getId();

    String getBrandTitle(Optional<String> locale);

    String getDefaultBrandingLogoURL(Optional<String> locale);

    String getGreyTransparentLogoURL(Optional<String> locale);
    
    String getSolutionsInSailingImageURL();
    
    String getSoutionsInSailingTrimmedImageURL();
    
    String getSailingRaceManagerAppTrimmedImageURL();
    
    String getSailInSightAppImageURL();
    
    String getSailingRaceManagerAppImageURL();
    
    String getSailingSimulatorImageURL();
    
    String getSailingSimulatorTrimmedImageURL();

    String getBuoyPingerAppImageURL();

    String getSailingAnalyticsImageURL();

    String getSailingAnalyticsReadMoreText(Optional<String> locale);

    String getSailingAnalyticsSailing(Optional<String> locale);
    
    String getFooterCopyright();
    
    String getFooterLegalLink();
    
    String getFooterPrivacyLink();
    
    String getFooterJobsLink();
    
    String getFooterSupportLink();
    
    String getFooterWhatsNewLink();
    
    String getSportsOn(Optional<String> locale);
    
    String getFollowSports(Optional<String> locale);
    
    String getFacebookLink();
    
    String getxLink();
    
    String getInstagramLink();
    
    String getWelcomeToSailingAnalytics(Optional<String> locale);
    
    String getWelcomeToSailingAnalyticsBody(Optional<String> locale);
    
    String getMoreLoginInformationNotificationsURL();
    
    String getMoreLoginInformationSettingsURL();
    
    String getMoreLoginInformationSailorProfilesURL();
    
    String getMoreLoginInformationSimulatorURL();
    
    String getInSailingContent(Optional<String> locale);
}
