package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;

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
                .build();
    }

    @Override
    public void configureUiBindings(final UiBindingRegistry uiBindingRegistry) {
        uiBindingRegistry.registerMouseDownBinding(
                new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
                new PopupMenuAction(this.bodyMenu));
    }
}
