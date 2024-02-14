package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.equinox.p2.operations.ProfileChangeOperation;

import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;

/**
 *
 * IErrorReportingPage is used to report resolution
 * errors on a wizard page.
 *
 * @since 3.5
 *
 */
public interface IResolutionErrorReportingPage extends ISelectableIUsPage {
	public void updateStatus(IUElementListRoot root, ProfileChangeOperation operation);
}
