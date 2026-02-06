package ro.linic.ui.base.services.ui;

import org.eclipse.nebula.widgets.opal.commons.SWTGraphicUtil;
import org.eclipse.swt.graphics.Color;

/**
 * This class is a simple POJO that holds colors used by the Notifier widget
 *
 */
class NotifierColors {
	Color titleColor;
	Color textColor;
	Color borderColor;
	Color leftColor;
	Color rightColor;

	void dispose() {
		SWTGraphicUtil.safeDispose(titleColor);
		SWTGraphicUtil.safeDispose(borderColor);
		SWTGraphicUtil.safeDispose(leftColor);
		SWTGraphicUtil.safeDispose(rightColor);
		SWTGraphicUtil.safeDispose(textColor);
	}
}
