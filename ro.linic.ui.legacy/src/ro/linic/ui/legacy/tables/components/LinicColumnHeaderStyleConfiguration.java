package ro.linic.ui.legacy.tables.components;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultColumnHeaderStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.BeveledBorderDecorator;

public class LinicColumnHeaderStyleConfiguration extends DefaultColumnHeaderStyleConfiguration
{
	public LinicColumnHeaderStyleConfiguration()
	{
		super();
		font = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		cellPainter = new BeveledBorderDecorator(new TextPainter(false, true, false, true));
	}
}
