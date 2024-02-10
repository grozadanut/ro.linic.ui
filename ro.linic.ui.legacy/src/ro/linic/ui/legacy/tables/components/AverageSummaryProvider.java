package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;

public class AverageSummaryProvider implements ISummaryProvider
{
	private final IDataProvider dataProvider;
	
	public AverageSummaryProvider(final IDataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
    }
	
	@Override
	public Object summarize(final int columnIndex)
	{
		double total = 0;
		final int rowCount = dataProvider.getRowCount();
		
		if (rowCount == 0)
			return 0d;

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
		{
			final Object dataValue = dataProvider.getDataValue(columnIndex, rowIndex);
			if (dataValue instanceof Number)
				total = total + ((Number) dataValue).doubleValue();
			
		}
		return total / rowCount;
	}
}