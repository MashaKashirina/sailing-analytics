package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SuggestBox;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

public class UpgradeArchiveServerDialog extends AbstractNewProcessDialog<UpgradeArchiveServerDialog.UpgradeArchiveServerInstructions> {
    
    public static class UpgradeArchiveServerInstructions extends AbstractNewProcessDialog.NewProcessInstructions {
        private final String releaseNameOrNullForLatestMaster;

        public UpgradeArchiveServerInstructions(String releaseNameOrNullForLatestMaster,
                String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalInstanceType,
                Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull) {
            super(optionalInstanceType, masterReplicationBearerToken, replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull);
            this.releaseNameOrNullForLatestMaster = releaseNameOrNullForLatestMaster;
        }

        public String getReleaseNameOrNullForLatestMaster() {
            return releaseNameOrNullForLatestMaster;
        }
    }
    
    private final SuggestBox releaseNameBox;

    public UpgradeArchiveServerDialog(LandscapeManagementWriteServiceAsync landscapeManagementService, String defaultInstanceType,
            Iterable<String> releaseNames,
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<UpgradeArchiveServerInstructions> callback) {
        super(stringMessages.upgradeArchiveServer(), landscapeManagementService, stringMessages, errorReporter, callback);
        releaseNameBox = LandscapeDialogUtil.createReleaseNameBox(stringMessages, releaseNames, this);
        if (defaultInstanceType != null) {
            for (int i=0; i<getInstanceTypeListBox().getItemCount(); i++) {
                if (getInstanceTypeListBox().getItemText(i).equals(defaultInstanceType)) {
                    getInstanceTypeListBox().setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    @Override
    protected Grid getAdditionalWidget() {
        final Grid result = super.getAdditionalWidget();
        int row=0;
        result.insertRow(row);
        result.setWidget(row, 0, new Label(stringMessages.release()));
        result.setWidget(row++, 1, releaseNameBox);
        return result;
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return releaseNameBox.getValueBox();
    }
    
    @Override
    protected UpgradeArchiveServerInstructions getResult() {
        return new UpgradeArchiveServerInstructions(LandscapeDialogUtil.getReleaseNameBoxValue(releaseNameBox, stringMessages), getMasterReplicationBearerTokenBox().getValue(),
                getReplicaReplicationBearerTokenBox().getValue(), getInstanceTypeListBox().getSelectedValue(),
                getMemoryInMegabytesBox().getValue(), getMemoryTotalSizeFactorBox().getValue()); 
    }
}
