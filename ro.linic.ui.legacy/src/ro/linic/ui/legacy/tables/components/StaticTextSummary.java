package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;

public class StaticTextSummary implements ISummaryProvider
{
	private String text;
	
	public StaticTextSummary(final String text)
	{
		this.text = text;
	}
	
    @Override public Object summarize(final int columnIndex)
    {
        return text;
    }
}