package ro.linic.ui.base.services.nattable.components;

import static ro.flexbiz.util.commons.StringUtils.notEmpty;

import java.util.List;
import java.util.Optional;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.widgets.Event;

import ro.linic.ui.base.services.nattable.Column;

public class FullFeaturedContentTooltip extends NatTableContentTooltip {
	private List<Column> columns;

	public FullFeaturedContentTooltip(final NatTable natTable, final List<Column> columns, final String... tooltipRegions) {
		super(natTable, tooltipRegions);
		this.columns = columns;
	}

	@Override
	protected String getText(final Event event) {
		final int col = this.natTable.getColumnPositionByX(event.x);
		final int row = this.natTable.getRowPositionByY(event.y);
		
		if (row == 0) {
			final Optional<String> tooltip = Optional.ofNullable(columns.get(col-1)) // -1 because of the row header column
					.map(Column::tooltip);
			
			if (tooltip.isPresent() && notEmpty(tooltip.get()))
				return tooltip.get();
		}

		return super.getText(event);
	}
}
