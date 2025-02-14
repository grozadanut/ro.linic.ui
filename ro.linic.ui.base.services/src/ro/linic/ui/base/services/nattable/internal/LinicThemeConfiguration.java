package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;

/**
 * ThemeConfiguration that sets different fonts which has impact on the row
 * heights and columns widths. The automatic resizing is done via specially
 * configured TextPainter instances.
 */
public class LinicThemeConfiguration extends ModernNatTableThemeConfiguration {
	{
		this.defaultFont = JFaceResources.getDefaultFont();
		this.defaultSelectionFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		this.cHeaderFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		this.cHeaderSelectionFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		this.rHeaderFont = JFaceResources.getDefaultFont();
		this.rHeaderSelectionFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		this.renderCornerGridLines = true;
		this.renderColumnHeaderGridLines = true;
		
		this.summaryRowFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		this.summaryRowHAlign = HorizontalAlignmentEnum.CENTER;
		
		this.dataChangeBgColor = GUIHelper.COLOR_RED;
	}
}