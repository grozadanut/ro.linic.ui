package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeCollapseAllCommand;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeExpandAllCommand;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class NatMenuItemProvider
{
	public static final String EXPAND_COLLAPSE_MENU_ITEM_ID = "expandCollapseMenuItem"; //$NON-NLS-1$
	
	public static IMenuItemProvider expandCollapseMenuItemProvider()
	{
		return new IMenuItemProvider()
		{
			@Override public void addMenuItem(final NatTable natTable, final Menu popupMenu)
			{
				final MenuItem expandMenuItem = new MenuItem(popupMenu, SWT.PUSH);
				expandMenuItem.setText("Expand All");
				expandMenuItem.setImage(GUIHelper.getImage("plus")); //$NON-NLS-1$
				expandMenuItem.setEnabled(true);

				expandMenuItem.addSelectionListener(new SelectionAdapter()
				{
					@Override public void widgetSelected(final SelectionEvent e)
					{
						natTable.doCommand(new TreeExpandAllCommand());
					}
				});
				
				final MenuItem collapseMenuItem = new MenuItem(popupMenu, SWT.PUSH);
				collapseMenuItem.setText("Collapse All");
				collapseMenuItem.setImage(GUIHelper.getImage("minus")); //$NON-NLS-1$
				collapseMenuItem.setEnabled(true);

				collapseMenuItem.addSelectionListener(new SelectionAdapter()
				{
					@Override public void widgetSelected(final SelectionEvent e)
					{
						natTable.doCommand(new TreeCollapseAllCommand());
					}
				});
			}
		};
	}
}
