package ro.linic.ui.p2.internal.ui.dialogs;

/**
 * IRepositoryManipulationHood defines callbacks that are called when the
 * UI is manipulating repositories.
 */
public interface IRepositoryManipulationHook {
	public void preManipulateRepositories();

	public void postManipulateRepositories();
}
