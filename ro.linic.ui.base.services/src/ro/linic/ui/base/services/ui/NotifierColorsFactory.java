package ro.linic.ui.base.services.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * This class creates the colors associated to a given theme
 * 
 */
public class NotifierColorsFactory {

	public enum NotifierTheme {
		FLEXBIZ_THEME, YELLOW_THEME, GRAY_THEME, BLUE_THEME
	};

	/**
	 * Constructor
	 */
	private NotifierColorsFactory() {

	}

	/**
	 * @param theme a theme for the notifier widget
	 * @return the color set for the given theme
	 */
	static NotifierColors getColorsForTheme(final NotifierTheme theme) {
		final NotifierColors colors = new NotifierColors();
		final Display display = Display.getDefault();
		switch (theme) {
		case BLUE_THEME:
			colors.textColor = new Color(display, 4, 64, 140);
			colors.titleColor = new Color(display, 0, 0, 0);
			colors.borderColor = new Color(display, 153, 188, 232);
			colors.leftColor = new Color(display, 210, 225, 244);
			colors.rightColor = new Color(display, 182, 207, 238);
			break;
		case GRAY_THEME:
			colors.textColor = new Color(display, 0, 0, 0);
			colors.titleColor = new Color(display, 255, 20, 20);
			colors.borderColor = new Color(display, 208, 208, 208);
			colors.leftColor = new Color(display, 255, 255, 255);
			colors.rightColor = new Color(display, 208, 208, 208);
			break;
		case YELLOW_THEME:
			colors.textColor = new Color(display, 0, 0, 0);
			colors.titleColor = new Color(display, 0, 0, 0);
			colors.borderColor = new Color(display, 218, 178, 85);
			colors.leftColor = new Color(display, 220, 220, 160);
			colors.rightColor = new Color(display, 255, 255, 191);
			break;
		default:
			colors.textColor = new Color(display, 0, 0, 0);
			colors.titleColor = new Color(display, 0, 0, 0);
			colors.borderColor = new Color(display, 27, 197, 189);
			colors.leftColor = new Color(display, 27, 197, 189);
			colors.rightColor = new Color(display, 220, 255, 253);
			break;
		}
		return colors;
	}

}
