package ro.linic.ui.legacy.tables.components;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;

public class SelectionSumQuantitiesSummary implements ISummaryProvider
{
	final private IDataProvider dataProvider;
	final private SelectionLayer selectionLayer;
	final private int uomColIndex;
	final private int quantityColIndex;
	
	public SelectionSumQuantitiesSummary(final IDataProvider dataProvider, final SelectionLayer selectionLayer,
			final int uomColIndex, final int quantityColIndex)
	{
		this.dataProvider = dataProvider;
		this.selectionLayer = selectionLayer;
		this.uomColIndex = uomColIndex;
		this.quantityColIndex = quantityColIndex;
	}
	
	@Override
	public Object summarize(final int columnIndex)
	{
		final boolean hasRowSelection = !selectionLayer.getSelectionModel().isEmpty();
		final int rowCount = dataProvider.getRowCount();
		final HashMap<String, BigDecimal> uomTotals = new HashMap<>();

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
		{
			if (hasRowSelection && !selectionLayer.isRowPositionSelected(rowIndex))
				continue;
			
			final String uom = safeString(dataProvider.getDataValue(uomColIndex, rowIndex), Object::toString, String::trim, String::toUpperCase);
			final BigDecimal quantity = (BigDecimal) dataProvider.getDataValue(quantityColIndex, rowIndex);
			if (quantity != null)
			{
				final BigDecimal totalSoFar = uomTotals.getOrDefault(uom, BigDecimal.ZERO);
				uomTotals.put(uom, totalSoFar.add(quantity));
			}
		}
		return uomTotals.entrySet().stream()
				.filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) != 0)
				.sorted(Comparator.comparing(Entry::getKey))
				.map(entry -> displayBigDecimal(entry.getValue()) + SPACE + entry.getKey())
				.collect(Collectors.joining(LIST_SEPARATOR));
	}
}
