package com.sap.sailing.gwt.home.shared.partials.checkboxtile;

import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;

public final class CheckBoxTile extends Composite {
    final Consumer<Boolean> onToggle;
    final boolean showLoadingIndicator;

    private static CheckBoxTileUiBinder uiBinder = GWT.create(CheckBoxTileUiBinder.class);

    interface CheckBoxTileUiBinder extends UiBinder<Widget, CheckBoxTile> {
    }

    @UiField
    SharedResources res;
    @UiField
    Label labelUi;
    @UiField
    CheckBox toggleButtonUi;

    /**
     * @param showLoadingIndicator
     *            Appears for the duration of onToggle execution. Blocks toggling while active. Applicable for async
     *            fields.
     */
    public CheckBoxTile(final boolean isEnabled, final String label, final boolean initialState,
            final Consumer<Boolean> onToggle, final boolean showLoadingIndicator) {
        super();
        this.showLoadingIndicator = showLoadingIndicator;
        this.onToggle = onToggle;
        initWidget(uiBinder.createAndBindUi(this));
        labelUi.setText(label);
        if (!isEnabled) {
            toggleButtonUi.setEnabled(false);
        }
        toggleButtonUi.setValue(initialState);
        toggleButtonUi.addValueChangeHandler(value -> {
            final Boolean newValue = value.getValue();
            if (showLoadingIndicator) {
                wrapRunWithLoadingIndicator(() -> {
                    onToggleWithToggleBlockedDuringExecution(newValue);
                });
            } else {
                onToggleWithToggleBlockedDuringExecution(newValue);
            }
        });
    }

    private void onToggleWithToggleBlockedDuringExecution(final boolean newVal) {
        toggleButtonUi.setEnabled(false);
        try {
            onToggle.accept(newVal);
            toggleButtonUi.setEnabled(true);
        } catch (Exception e) {
            toggleButtonUi.setValue(!newVal);
            toggleButtonUi.setEnabled(true);
            throw e;
        }
    }

    private void wrapRunWithLoadingIndicator(final Runnable r) {
        try {
            // TODO activate loading indicator
            r.run();
            // TODO deactivate loading indicator
        } catch (Exception e) {
            // TODO deactivate loading indicator
            throw e;
        }
    }
}

// /// **
// * UI component for a list of tiles with a checkbox and label.
// */
// public final class CheckList extends Composite {
//
// private static CheckListUiBinder uiBinder = GWT.create(CheckListUiBinder.class);
//
// interface CheckListUiBinder extends UiBinder<Widget, CheckList> {
// }
//
// @UiField
// SpanElement headerTitleUi;
// @UiField
// FlowPanel notificationToggleContainerUi;
// @UiField
// FlowPanel itemContainerUi;
// final List<CheckListTile> checkListTiles;
//
// class CheckListTile {
// final String label;
// final boolean initialValue;
// final Predicate<Boolean> onToggle;
// /** Appears for the duration of onToggle execution. Blocks toggling while active. Applicable for async fields. */
// final boolean showLoadingIndicator;
//
// public CheckListTile(String label, boolean initialValue, Predicate<Boolean> onToggle, boolean isAsync) {
// super();
// this.label = label;
// this.initialValue = initialValue;
// this.onToggle = onToggle;
// this.showLoadingIndicator = isAsync;
// }
//
// SuggestedMultiSelectionNotificationToggle composeUI() {
// final SuggestedMultiSelectionNotificationToggle tile = new SuggestedMultiSelectionNotificationToggle(label);
// tile.setValue(initialValue);
// tile.addValueChangeHandler(newValue -> onToggle.test(newValue.getValue()));
// return tile;
// }
// }
//
// private CheckList(String title, List<CheckListTile> checkListTiles) {
// this.checkListTiles = checkListTiles;
// SuggestedMultiSelectionResources.INSTANCE.css().ensureInjected();
// initWidget(uiBinder.createAndBindUi(this));
// headerTitleUi.setInnerText(title);
// }
// }
