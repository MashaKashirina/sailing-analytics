package com.sap.sailing.gwt.ui.shared;

import com.sap.sailing.gwt.common.communication.event.EventReferenceDTO;

public class EventLinkDTO extends EventReferenceDTO {
    
    private String baseURL;
    private boolean isOnRemoteServer;
    
    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public boolean isOnRemoteServer() {
        return isOnRemoteServer;
    }

    public void setOnRemoteServer(boolean isOnRemoteServer) {
        this.isOnRemoteServer = isOnRemoteServer;
    }

}
