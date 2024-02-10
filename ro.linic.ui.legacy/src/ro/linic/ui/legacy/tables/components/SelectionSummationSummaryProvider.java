package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummationSummaryProvider;

public class SelectionSummationSummaryProvider implements ISummaryProvider
{
	private final IDataProvider dataProvider;
	private final SelectionLayer selectionLayer;
	private final boolean strict;

	/**
	 * Create a new {@link SummationSummaryProvider} by using the given
	 * {@link IDataProvider}.
	 * <p>
	 * Using this constructor will set the {@link SummationSummaryProvider} in
	 * strict mode which means that if a column contains non Number values,
	 * {@link ISummaryProvider#DEFAULT_SUMMARY_VALUE} will be returned.
	 *
	 * @param dataProvider The {@link IDataProvider} that should be used to
	 *                     calculate the sum.
	 */
	public SelectionSummationSummaryProvider(final IDataProvider dataProvider, final SelectionLayer selectionLayer)
	{
		this(dataProvider, selectionLayer, true);
	}

	/**
	 * Create a new {@link SummationSummaryProvider} by using the given
	 * {@link IDataProvider} and strict mode configuration.
	 * <p>
	 * Using this constructor will set the {@link SummationSummaryProvider} in
	 * strict mode which means that if a column contains non Number values,
	 * {@link ISummaryProvider#DEFAULT_SUMMARY_VALUE} will be returned.
	 *
	 * @param dataProvider The {@link IDataProvider} that should be used to
	 *                     calculate the sum.
	 * @param strict       If strict is set to <code>true</code> and one or more of
	 *                     the values in the column is not of type Number, then
	 *                     {@link ISummaryProvider#DEFAULT_SUMMARY_VALUE} will be
	 *                     returned. If strict is set to <code>false</code>, this
	 *                     method will return the sum of all the values in the
	 *                     column that are of type Number, ignoring the non Number
	 *                     values.
	 */
	public SelectionSummationSummaryProvider(final IDataProvider dataProvider, final SelectionLayer selectionLayer,
			final boolean strict)
	{
		this.dataProvider = dataProvider;
		this.selectionLayer = selectionLayer;
		this.strict = strict;
	}

	/**
	 * Calculates the sum of the values in the column.
	 *
	 * @return The sum of all Number values in the column as Double or
	 *         {@link ISummaryProvider#DEFAULT_SUMMARY_VALUE} if the column contains
	 *         non Number values and this SummationSummaryProvider is configured to
	 *         be strict.
	 */
	@Override
	public Object summarize(final int columnIndex)
	{
		final boolean hasRowSelection = !selectionLayer.getSelectionModel().isEmpty();
		final int rowCount = this.dataProvider.getRowCount();
		double summaryValue = 0;

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
		{
			if (hasRowSelection && !selectionLayer.isRowPositionSelected(rowIndex))
				continue;

			final Object dataValue = this.dataProvider.getDataValue(columnIndex, rowIndex);
			if (dataValue instanceof Number)
				summaryValue += ((Number) dataValue).doubleValue();
			else if (this.strict)
				return DEFAULT_SUMMARY_VALUE;
		}

		return summaryValue;
	}
}
