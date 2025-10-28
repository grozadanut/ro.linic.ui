package ro.linic.ui.base.services.nattable.components;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;
import org.eclipse.swt.events.MouseEvent;

public class NonEmptyMouseEventMatcher extends CellLabelMouseEventMatcher {
	public NonEmptyMouseEventMatcher(final String regionName, final int button, final String labelToMatch) {
		super(regionName, button, labelToMatch);
	}

	@Override
	public boolean matches(final NatTable natTable, final MouseEvent event, final LabelStack regionLabels) {
		final ILayerCell cell = natTable.getCellByPosition(
                natTable.getColumnPositionByX(event.x),
                natTable.getRowPositionByY(event.y));

		if (cell == null)
			return false;

		return super.matches(natTable, event, regionLabels)
				&& ObjectUtils.isNotNull(cell.getDataValue());
	}
}
