package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.function.BiConsumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.places.user.profile.preferences.UserPreferencesView;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public class MiscellaneousDisplayImpl {
    public final LabeledBox selectionUi;

    public MiscellaneousDisplayImpl(final UserPreferencesView.Presenter presenter) {
        // compose ui
        final String securityUpdatesTitle = StringMessages.INSTANCE.securityUpdates();
        final CheckBoxTile securityUpdates = new CheckBoxTile(securityUpdatesTitle, true, null);
        final CheckBoxTile featureAndCommunityUpdates = composeFeatureAndCommunityUpdatesTile(presenter);
        final FlowPanel tileList = new FlowPanel();
        tileList.add(securityUpdates);
        tileList.add(featureAndCommunityUpdates);
        final String boxTitle = StringMessages.INSTANCE.miscellaneous();
        selectionUi = new LabeledBox(boxTitle, tileList);
        // fire init call
        presenter.initIsSubscribedToFeatureAndCommunityUpdates(featureAndCommunityUpdates);
    }

    private AsyncCallback<VoidResult> wrapCallbackWithToastResponse(final boolean isNowTrue,
            final AsyncCallback<VoidResult> callback) {
        final AsyncCallback<VoidResult> callbackWrappedWithToastNotification = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                final String failText = StringMessages.INSTANCE.failedToSetStatusOfFeatureAndCommunityUpdates();
                Notification.notify(failText, NotificationType.ERROR);
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(VoidResult result) {
                final String passAndTrue = StringMessages.INSTANCE.youWillNowReceiveFeatureAndCommunityUpdates();
                final String passAndFalse = StringMessages.INSTANCE
                        .youWillNotReceiveFeatureAndCommunityUpdatesAnymore();
                final String message = isNowTrue ? passAndTrue : passAndFalse;
                Notification.notify(message, NotificationType.SUCCESS);
                callback.onSuccess(result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        };
        return callbackWrappedWithToastNotification;
    }

    private CheckBoxTile composeFeatureAndCommunityUpdatesTile(final UserPreferencesView.Presenter presenter) {
        final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (isNowTrue, callback) -> {
            final AsyncCallback<VoidResult> wrappedCallback = wrapCallbackWithToastResponse(isNowTrue, callback);
            presenter.setIsSubscribedToFeatureAndCommunityUpdates(isNowTrue, wrappedCallback);
        };
        final String title = StringMessages.INSTANCE.featureAndCommunityUpdates();
        return new CheckBoxTile(title, false, onToggle);
    }
}