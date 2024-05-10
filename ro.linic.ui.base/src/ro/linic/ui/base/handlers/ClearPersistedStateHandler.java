 
package ro.linic.ui.base.handlers;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import ro.linic.ui.base.Messages;
import ro.linic.ui.base.dialogs.PromptRestartDialog;

public class ClearPersistedStateHandler {
	private static final ILog log = ILog.of(ClearPersistedStateHandler.class);
	
	@Execute
	public void execute(final Shell shell, final IWorkbench workbench) {
		if (!MessageDialog.openQuestion(shell, Messages.ClearPersistedState, Messages.ClearPersistedStateMessage))
			return;
		
		try {
			final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
			node.putBoolean(IWorkbench.CLEAR_PERSISTED_STATE, true);
			node.flush();
		} catch (final BackingStoreException e) {
			log.error(e.getMessage(), e);
		}
		
		final int retCode = PromptRestartDialog.promptForRestart(shell, true);
		if (retCode == PromptRestartDialog.PROFILE_RESTART)
			workbench.restart();
	}
}