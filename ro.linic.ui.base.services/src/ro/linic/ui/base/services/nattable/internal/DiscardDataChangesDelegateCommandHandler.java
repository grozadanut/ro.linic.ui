package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.nebula.widgets.nattable.command.ILayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.command.DiscardDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;

public class DiscardDataChangesDelegateCommandHandler implements ILayerCommandHandler<DiscardDataChangesCommand> {
    private final DataChangeLayer dataChangeLayer;
    private final MDirtyable dirtyable;

    /**
     *
     * @param dataChangeLayer
     *            The {@link DataChangeLayer} to which this command handler
     *            should be registered to.
     */
    public DiscardDataChangesDelegateCommandHandler(final DataChangeLayer dataChangeLayer, final MDirtyable dirtyable) {
        this.dataChangeLayer = dataChangeLayer;
        this.dirtyable = dirtyable;
    }

    @Override
    public boolean doCommand(final ILayer targetLayer, final DiscardDataChangesCommand command) {
        this.dataChangeLayer.discardDataChanges();
        if (dirtyable != null)
			dirtyable.setDirty(this.dataChangeLayer.isDirty());
        return true;
    }

    @Override
    public Class<DiscardDataChangesCommand> getCommandClass() {
        return DiscardDataChangesCommand.class;
    }
}
