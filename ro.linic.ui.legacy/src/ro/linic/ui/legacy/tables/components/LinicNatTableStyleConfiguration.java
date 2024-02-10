package ro.linic.ui.legacy.tables.components;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.LineBorderDecorator;

public class LinicNatTableStyleConfiguration extends DefaultNatTableStyleConfiguration
{
	public LinicNatTableStyleConfiguration()
	{
		super();
		font = JFaceResources.getDefaultFont();
		cellPainter = new LineBorderDecorator(new TextPainter(false, true, false, true));
	}
}
