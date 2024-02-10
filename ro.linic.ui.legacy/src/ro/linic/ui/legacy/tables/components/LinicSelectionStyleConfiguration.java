package ro.linic.ui.legacy.tables.components;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;

public class LinicSelectionStyleConfiguration extends DefaultSelectionStyleConfiguration
{
	public LinicSelectionStyleConfiguration()
	{
		super();
		selectionFont = JFaceResources.getDefaultFont();
		selectedHeaderFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		
		selectionBgColor = GUIHelper.COLOR_LIST_SELECTION;
		selectionFgColor = GUIHelper.COLOR_WHITE;
		anchorBgColor = GUIHelper.COLOR_LIST_SELECTION;
		anchorFgColor = GUIHelper.COLOR_WHITE;
		anchorBorderStyle = null;
	}
}
