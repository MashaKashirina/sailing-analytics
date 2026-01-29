package com.sap.sailing.gwt.home.shared.partials.checkboxtile;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * @param onToggle
 *            if null, toggle will be disabled
 */
public final class CheckBoxTile extends Composite {
    private static CheckBoxTileUiBinder uiBinder = GWT.create(CheckBoxTileUiBinder.class);

    interface CheckBoxTileUiBinder extends UiBinder<Widget, CheckBoxTile> {
    }

    @UiField
    CheckboxTileResources res;
    @UiField
    Label labelUi;
    @UiField
    CheckBox toggleButtonUi;
    private DivElement loadingOverlay;

    public CheckBoxTile(final String label, final boolean initialState,
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle) {
        super();
        initWidget(uiBinder.createAndBindUi(this));
        labelUi.setText(label);
        initToggleButtonUi(initialState, onToggle);
    }

    private void initToggleButtonUi(final boolean initialState,
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle) {
        toggleButtonUi.setValue(initialState);
        if (onToggle == null) {
            toggleButtonUi.setEnabled(false);
        }
        toggleButtonUi.getElement().getStyle().setProperty("position", "relative");
        toggleButtonUi.addValueChangeHandler(value -> {
            final Boolean newlyToggledValue = value.getValue();
            overlayLoadingSpinner();
            onToggle.accept(newlyToggledValue, callback());
        });
    }

    public void overlayLoadingSpinner() {
        toggleButtonUi.setEnabled(false);
        if (loadingOverlay == null) {
            initLoadingOverlay();
        }
        loadingOverlay.getStyle().setProperty("display", "flex");
    }

    private void initLoadingOverlay() {
        // add elements
        loadingOverlay = Document.get().createDivElement();
        loadingOverlay.addClassName(res.css().loadingOverlay());
        final DivElement spinner = Document.get().createDivElement();
        spinner.addClassName(res.css().spinner());
        // add to canvas
        loadingOverlay.appendChild(spinner);
        toggleButtonUi.getElement().appendChild(loadingOverlay);
    }

    public void hideLoadingSpinner() {
        if (loadingOverlay != null) {
            loadingOverlay.getStyle().setProperty("display", "none");
            toggleButtonUi.setEnabled(true);
        }
    }

    public void setValue(final boolean b) {
        toggleButtonUi.setValue(b);
    }

    public AsyncCallback<VoidResult> callback() {
        return new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                toggleButtonUi.setValue(!toggleButtonUi.getValue(), false); // undo failed toggle
                hideLoadingSpinner();
            }

            @Override
            public void onSuccess(VoidResult result) {
                hideLoadingSpinner();
            }
        };
    }
}
