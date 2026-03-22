<<<<<<<< HEAD:java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/common/communication/event/EventReferenceDTO.java
package com.sap.sailing.gwt.common.communication.event;
========
package com.sap.sailing.gwt.ui.shared;
>>>>>>>> origin/main:java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/ui/shared/EventReferenceDTO.java

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.base.EventBase;
<<<<<<<< HEAD:java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/common/communication/event/EventReferenceDTO.java
========
import com.sap.sse.gwt.shared.DTO;
>>>>>>>> origin/main:java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/ui/shared/EventReferenceDTO.java

public class EventReferenceDTO implements IsSerializable {
    
    private UUID id;
    private String displayName;
    
    public EventReferenceDTO() {
    }
    
    @GwtIncompatible
    public EventReferenceDTO(EventBase event) {
        this.id = (UUID) event.getId();
        this.displayName = event.getName();
    }

    public EventReferenceDTO(UUID id, String displayName) {
        super();
        this.id = id;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String name) {
        this.displayName = name;
    }
}
