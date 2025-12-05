package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;

public class DeviceConfigurationListComposite extends Composite  {
    protected static AdminConsoleTableResources tableResource = GWT.create(AdminConsoleTableResources.class);
    
    private final RefreshableMultiSelectionModel<DeviceConfigurationWithSecurityDTO> refreshableConfigurationSelectionModel;
    private final CellTable<DeviceConfigurationWithSecurityDTO> configurationTable;
    protected ListDataProvider<DeviceConfigurationWithSecurityDTO> configurationsDataProvider;
    
    private final SimplePanel mainPanel;
    private final VerticalPanel panel;

    private final Label noConfigurationsLabel;

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    protected final StringMessages stringMessages;

    public DeviceConfigurationListComposite(SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter,
            StringMessages stringMessages, final UserService userService) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        mainPanel = new SimplePanel();
        panel = new VerticalPanel();
        mainPanel.setWidget(panel);
        noConfigurationsLabel = new Label(stringMessages.noConfigurations());
        noConfigurationsLabel.setWordWrap(false);
        panel.add(noConfigurationsLabel);
        configurationsDataProvider = new ListDataProvider<DeviceConfigurationWithSecurityDTO>();
        refreshableConfigurationSelectionModel = new RefreshableMultiSelectionModel<>(
                new EntityIdentityComparator<DeviceConfigurationWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(DeviceConfigurationWithSecurityDTO dto1,
                            DeviceConfigurationWithSecurityDTO dto2) {
                        return Util.equalsWithNull(dto1.id, dto2.id);
                    }

                    @Override
                    public int hashCode(DeviceConfigurationWithSecurityDTO t) {
                        return t.id == null ? 0 : t.id.hashCode();
                    }
                }, configurationsDataProvider);
        configurationTable = createConfigurationTable(userService);
        configurationTable.setVisible(true);
        configurationTable.setSelectionModel(refreshableConfigurationSelectionModel);
        panel.add(configurationTable);
        initWidget(mainPanel);
        refreshTable();
    }

    public void refreshTable() {
        sailingServiceWrite.getDeviceConfigurations(new AsyncCallback<List<DeviceConfigurationWithSecurityDTO>>() {
            @Override
            public void onSuccess(List<DeviceConfigurationWithSecurityDTO> result) {
                if (configurationsDataProvider.getList().isEmpty()) {
                    configurationTable.getColumnSortList().clear();
                    configurationTable.getColumnSortList().push(configurationTable.getColumn(0));
                }
                configurationsDataProvider.getList().clear();
                configurationsDataProvider.getList().addAll(result);
                ColumnSortEvent.fire(configurationTable, configurationTable.getColumnSortList());
                noConfigurationsLabel.setVisible(false);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                noConfigurationsLabel.setText(stringMessages.errorRetrievingConfiguration());
                noConfigurationsLabel.setVisible(true);
                configurationTable.setVisible(false);
                errorReporter.reportError("Error retrieving configuration data from server: " + caught.getMessage());
                refreshableConfigurationSelectionModel.clear();
            }
        });
    }

    public RefreshableMultiSelectionModel<DeviceConfigurationWithSecurityDTO> getSelectionModel() {
        return refreshableConfigurationSelectionModel;
    }

    private CellTable<DeviceConfigurationWithSecurityDTO> createConfigurationTable(final UserService userService) {
        CellTable<DeviceConfigurationWithSecurityDTO> table = new BaseCelltable<DeviceConfigurationWithSecurityDTO>(
                /* pageSize */10000, tableResource);
        configurationsDataProvider.addDataDisplay(table);
        table.ensureDebugId("DeviceConfigurationList");
        table.setWidth("100%");
        ListHandler<DeviceConfigurationWithSecurityDTO> columnSortHandler = new ListHandler<DeviceConfigurationWithSecurityDTO>(
                configurationsDataProvider.getList());
        table.addColumnSortHandler(columnSortHandler);
        CheckboxCell headerCheckboxCell = new CheckboxCell();
        Header<Boolean> selectAllHeader = new Header<Boolean>(headerCheckboxCell) {
            private boolean checked = false;
            @Override
            public Boolean getValue() {
                return checked;
            }
        };
        selectAllHeader.setUpdater(value -> {
            List<DeviceConfigurationWithSecurityDTO> visible = configurationsDataProvider.getList();
            for (DeviceConfigurationWithSecurityDTO cfg : visible) {
                refreshableConfigurationSelectionModel.setSelected(cfg, value);
            }
            value = !value;
        });
        Column<DeviceConfigurationWithSecurityDTO, Boolean> selectionColumn =
                new Column<DeviceConfigurationWithSecurityDTO, Boolean>(new CheckboxCell(true, false)) {
                    @Override
                    public Boolean getValue(DeviceConfigurationWithSecurityDTO object) {
                        return refreshableConfigurationSelectionModel.isSelected(object);
                    }
                };
        selectionColumn.setFieldUpdater((index, object, value) ->
                refreshableConfigurationSelectionModel.setSelected(object, value));
        selectionColumn.setSortable(false);
        table.addColumn(selectionColumn, selectAllHeader);
        TextColumn<DeviceConfigurationWithSecurityDTO> identifierNameColumn = new TextColumn<DeviceConfigurationWithSecurityDTO>() {
            @Override
            public String getValue(DeviceConfigurationWithSecurityDTO config) {
                return config.name;
            }
        };
        identifierNameColumn.setSortable(true);
        columnSortHandler.setComparator(identifierNameColumn, (r1, r2) -> r1.name.compareTo(r2.name));
        table.addColumn(identifierNameColumn, stringMessages.device());
        final TextColumn<DeviceConfigurationWithSecurityDTO> deviceConfigurationUUIDColumn = new AbstractSortableTextColumn<DeviceConfigurationWithSecurityDTO>(
                config -> config.id == null ? "<null>" : config.id.toString(), columnSortHandler);
        final HasPermissions type = SecuredSecurityTypes.USER_GROUP;
        final AccessControlledActionsColumn<DeviceConfigurationWithSecurityDTO, DefaultActionsImagesBarCell> actionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionColumn.addAction(ACTION_DELETE, DELETE, config -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveDeviceConfiguration(config.getName()))) {
                sailingServiceWrite.removeDeviceConfiguration(config.id, new AsyncCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        refreshTable();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(caught.getMessage());
                    }
                });
            }
        });
        final EditOwnershipDialog.DialogConfig<DeviceConfigurationWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, user -> refreshTable(), stringMessages);
        final EditACLDialog.DialogConfig<DeviceConfigurationWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, user -> user.getAccessControlList(), stringMessages);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, DefaultActions.CHANGE_OWNERSHIP,
                configOwnership::openOwnershipDialog);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                u -> configACL.openDialog(u));
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(deviceConfigurationUUIDColumn, stringMessages.id());
        table.addColumn(actionColumn, stringMessages.actions());
        return table;
    }

    void update(DeviceConfigurationWithSecurityDTO configurationToUpdate) {
        final List<DeviceConfigurationWithSecurityDTO> configList = configurationsDataProvider.getList();
        for (int i=0; i<configList.size(); i++) {
            if (configList.get(i).id.equals(configurationToUpdate.id)) {
                configList.set(i, configurationToUpdate);
                break;
            }
        }
    }
}
