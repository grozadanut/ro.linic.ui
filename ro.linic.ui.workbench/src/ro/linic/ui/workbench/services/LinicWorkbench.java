package ro.linic.ui.workbench.services;

public interface LinicWorkbench {
	/**
	 * Replaces the program argument with the specified key with the given value 
	 * in the eclipse.ini file. If the key was not found, it appends it to the file.
	 * </br></br>
	 * NOTE: this change requires an application restart, which is NOT performed by this method
	 * 
	 * @param key case insensitive when finding
	 * @param value case insensitive when replacing
	 * @return true if the key was replaced or appended; false if the key already had the same value(case insensitive), or on writting error
	 */
	boolean replaceProgramArgument(String key, String value);
	/**
	 * Switches the workspace to the given location and restarts the workbench preserving authentication. 
	 * If the passed workspace location is the same as current workspace, does nothing.
	 * 
	 * @param workspaceLoc the new workspace location
	 */
	void switchWorkspace(String workspaceLoc);
	/**
	 * Restarts the workbench. The only advantage over <code>org.eclipse.e4.ui.workbench.IWorkbench.restart()</code> 
	 * is that it can be called before the workbench is initialized and it will restart when ready.
	 */
	void lazyRestart();
}
