package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.CellVisualChangeEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;

/**
 * Event handler that tracks data changes on the NatTable and updates 
 * the {@link MDirtyable} dirty state according to the state in the 
 * NatTable {@link DataChangeLayer}.
 */
public class MDirtyableUpdateDataChangeHandler implements ILayerListener {
	private final DataChangeLayer layer;
	private final MDirtyable dirtyable;

	public MDirtyableUpdateDataChangeHandler(final DataChangeLayer layer, final MDirtyable dirtyable) {
		this.layer = layer;
		this.dirtyable = dirtyable;
	}

	@Override
	public void handleLayerEvent(final ILayerEvent event) {
		if (event instanceof CellVisualChangeEvent)
			dirtyable.setDirty(layer.isDirty());
	}
}
