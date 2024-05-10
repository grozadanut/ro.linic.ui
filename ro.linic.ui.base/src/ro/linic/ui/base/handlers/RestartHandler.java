package ro.linic.ui.base.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;

public class RestartHandler {
	@Execute
	public void execute(final IWorkbench workbench) {
		workbench.restart();
	}
}
