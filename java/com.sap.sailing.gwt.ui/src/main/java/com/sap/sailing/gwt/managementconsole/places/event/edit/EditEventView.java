package com.sap.sailing.gwt.managementconsole.places.event.edit;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.ui.RequiresResize;
import com.sap.sailing.gwt.managementconsole.mvp.View;
import com.sap.sailing.gwt.ui.shared.EventDTO;

public interface EditEventView extends View<EditEventView.Presenter>, RequiresResize {

    void populateForm(EventDTO event);

    interface Presenter extends com.sap.sailing.gwt.managementconsole.mvp.Presenter {
        void saveEvent(String name, String description, String venue, Date startDate, Date endDate,
                List<String> courseAreaNames, boolean isPublic);
        void cancelEdit();
    }

}
