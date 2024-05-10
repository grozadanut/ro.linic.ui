package ro.linic.ui.base;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName()+".messages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	public static String ProgressMonitorControl_RunningTasks;
	public static String PromptRestartDialog_Restart;
	public static String PromptRestartDialog_NotYet;
	public static String PromptRestartDialog_ApplyChanges;
	public static String PromptRestartDialog_Title;
	public static String ApplicationInRestartDialog;
	public static String PlatformRestartMessage;
	public static String OptionalPlatformRestartMessage;
	public static String ClearPersistedState;
	public static String ClearPersistedStateMessage;
	public static String About;
}
