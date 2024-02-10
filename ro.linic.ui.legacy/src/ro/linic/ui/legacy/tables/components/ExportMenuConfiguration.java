package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.export.command.ExportCommand;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

//[1] IConfiguration for registering a UI binding to open a menu
public class ExportMenuConfiguration extends AbstractUiBindingConfiguration
{
	private final Menu menu;

	public ExportMenuConfiguration(final NatTable natTable)
	{
        // [2] create the menu using the PopupMenuBuilder
        this.menu = new PopupMenuBuilder(natTable)
        			.withMenuItemProvider(exportToExcelMenuItemProvider())
                    .build();
    }

	@Override
	public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
	{
		// [3] bind the PopupMenuAction to a right click
		// using GridRegion.COLUMN_HEADER instead of null would
		// for example open the menu only on performing a right
		// click on the column header instead of any region
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
				new PopupMenuAction(this.menu));
	}
	
	public static IMenuItemProvider exportToExcelMenuItemProvider() {
        return new IMenuItemProvider() {

            @Override
            public void addMenuItem(final NatTable natTable, final Menu popupMenu) {
                final MenuItem exportToImage = new MenuItem(popupMenu, SWT.PUSH);
                exportToImage.setText("Export to Excel");
                exportToImage.setEnabled(true);

                exportToImage.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                    	natTable.doCommand(new ExportCommand(natTable.getConfigRegistry(), natTable.getShell()));
                    }
                });
            }
        };
    }
}
