package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.URI;

/**
 * Listener for the repository selection combo.  Whenever the selected repository changes (menu selection,
 * text modified, new repo added) this listener will be notified.
 *
 * @since 3.5
 */
public interface IRepositorySelectionListener {
	/**
	 * Called whenever the selected repository in the combo changes.
	 *
	 * @param repoChoice one of AvailableIUGroup.AVAILABLE_NONE, AvailableIUGroup.AVAILABLE_ALL, AvailableIUGroup.AVAILABLE_LOCAL, AvailableIUGroup.AVAILABLE_SPECIFIED
	 * @param repoLocation if the repoChoice is set to AvailableIUGroup.AVAILABLE_SPECIFIED, this field will contain the URI of the selected repo, otherwise <code>null</code>
	 */
	public void repositorySelectionChanged(int repoChoice, URI repoLocation);
}