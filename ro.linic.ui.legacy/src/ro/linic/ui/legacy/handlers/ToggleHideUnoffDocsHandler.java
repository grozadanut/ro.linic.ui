package ro.linic.ui.legacy.handlers;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.legacy.session.ClientSession;

public class ToggleHideUnoffDocsHandler
{
	@Inject private Logger log;
	
	@Execute
	public void execute(final Shell shell)
	{
		ClientSession.instance().toggleHideUnofficialDocs(log);
	}
}
