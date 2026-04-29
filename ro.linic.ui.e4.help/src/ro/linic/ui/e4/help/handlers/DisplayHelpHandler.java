
package ro.linic.ui.e4.help.handlers;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Execute;

public class DisplayHelpHandler {
	private static final ILog log = ILog.of(DisplayHelpHandler.class);
//	@Execute
//	public void execute(final Shell shell, final UserGuide userGuide) {
//		userGuide.allTutorialContent().ifPresent(html -> new BrowserDialog(shell, Messages.UserGuide, html).open());
//	}

	@Execute
	public void execute() throws Exception {
		openUrl("https://bookstack.flexbiz.ro/books/manual-flexbiz");
	}
	
	public static void openUrl(final String url) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
				Desktop.getDesktop().browse(new URI(url));
		} catch (IOException | URISyntaxException e) {
			log.error(e.getMessage(), e);
		}
	}
}