package ro.linic.ui.base.services.nattable.internal;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;

import ro.flexbiz.util.commons.HeterogeneousDataComparator;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.components.NonEmptyMouseEventMatcher;

public class CustomGeneralConfiguration<T> extends AbstractRegistryConfiguration {
	final private List<Column> columns;
	final private ListDataProvider<T> bodyDataProvider;
	final private Map<Column, BiConsumer<T, Object>> clickConsumers;
	
	public CustomGeneralConfiguration(final List<Column> columns, final ListDataProvider<T> bodyDataProvider,
			final Map<Column, BiConsumer<T, Object>> clickConsumers) {
		this.columns = columns;
		this.bodyDataProvider = bodyDataProvider;
		this.clickConsumers = clickConsumers;
	}

	@Override
	public void configureRegistry(final IConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(
                FilterRowConfigAttributes.FILTER_COMPARATOR,
                HeterogeneousDataComparator.INSTANCE);
		
		configRegistry.registerConfigAttribute(
                SortConfigAttributes.SORT_COMPARATOR,
                HeterogeneousDataComparator.INSTANCE);
	}
	
	@Override
	public void configureUiBindings(final UiBindingRegistry uiBindingRegistry) {
		clickConsumers.forEach((column, consumer) -> {
			final CellLabelMouseEventMatcher leftClickEventMatcher = new NonEmptyMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(column));
			uiBindingRegistry.registerFirstSingleClickBinding(leftClickEventMatcher, new ClickConverter(consumer));
			
			final CellLabelMouseEventMatcher mouseHoverMatcher = new NonEmptyMouseEventMatcher(GridRegion.BODY, 0,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(column));
			// show hand cursor, which is usually used for links
			uiBindingRegistry.registerFirstMouseMoveBinding(mouseHoverMatcher, 
					(natTable, event) -> natTable.setCursor(natTable.getDisplay().getSystemCursor(SWT.CURSOR_HAND)), 
					(natTable, event) -> natTable.setCursor(null));
		});
	}
	
	private class ClickConverter implements IMouseAction {
		final private BiConsumer<T, Object> consumer;
		
		public ClickConverter(final BiConsumer<T, Object> consumer) {
			super();
			this.consumer = consumer;
		}

		@Override
		public void run(final NatTable natTable, final MouseEvent event) {
			final NatEventData eventData = NatEventData.createInstanceFromEvent(event);
			final int columnIndex = natTable.getColumnIndexByPosition(eventData.getColumnPosition());
			final int rowIndex = natTable.getRowIndexByPosition(eventData.getRowPosition());

			final T rowObject = bodyDataProvider.getRowObject(rowIndex);
	        final Object cellDataValue = bodyDataProvider.getDataValue(columnIndex, rowIndex);
	        consumer.accept(rowObject, cellDataValue);
	        
		}
	}
}