
package ro.linic.ui.e4.help.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.help.internal.base.BaseHelpSystem;

public class DisplayHelpHandler {
//	@Execute
//	public void execute(final Shell shell, final UserGuide userGuide) {
//		userGuide.allTutorialContent().ifPresent(html -> new BrowserDialog(shell, Messages.UserGuide, html).open());
//	}

	@Execute
	public void execute() throws Exception {
		// hardcoded for now
		BaseHelpSystem.getHelpDisplay().displayHelpResource("/ro.linic.ui.legacy/html/toc.html", true);
	}
}