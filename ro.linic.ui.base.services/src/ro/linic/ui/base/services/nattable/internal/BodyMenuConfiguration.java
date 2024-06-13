package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.export.command.ExportCommand;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.osgi.framework.FrameworkUtil;

import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.util.UIUtils;

public class BodyMenuConfiguration extends AbstractUiBindingConfiguration {
    private final Menu bodyMenu;

    /**
     * @param natTable
     *            The {@link NatTable} instance to register the body menu to.
     * @since 1.4
     */
    public BodyMenuConfiguration(final NatTable natTable) {
        this.bodyMenu = new PopupMenuBuilder(natTable)
                .withColumnStyleEditor("%ColumnStyleEditorDialog.shellTitle") //$NON-NLS-1$
                .withFreezeColumnMenuItem()
                .withFreezePositionMenuItem(false)
                .withUnfreezeMenuItem()
                .withAutoResizeSelectedColumnsMenuItem()
                .withAutoResizeSelectedRowsMenuItem()
                .withMenuItemProvider(exportToExcelMenuItemProvider())
                .build();
    }

    @Override
    public void configureUiBindings(final UiBindingRegistry uiBindingRegistry) {
        uiBindingRegistry.registerMouseDownBinding(
                new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
                new PopupMenuAction(this.bodyMenu));
    }
    
    public static IMenuItemProvider exportToExcelMenuItemProvider() {
        return new IMenuItemProvider() {
            @Override
            public void addMenuItem(final NatTable natTable, final Menu popupMenu) {
                final MenuItem exportToExcel = new MenuItem(popupMenu, SWT.PUSH);
                exportToExcel.setText(Messages.ExportExcel);
                UIUtils.find(FrameworkUtil.getBundle(getClass()), "icons/excel_16x16.png") //$NON-NLS-1$
                .ifPresent(url -> exportToExcel.setImage(GUIHelper.getImageByURL(url)));
                exportToExcel.setEnabled(true);
                exportToExcel.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                    	natTable.doCommand(new ExportCommand(natTable.getConfigRegistry(), natTable.getShell()));
                    }
                });
            }
        };
    }
}
