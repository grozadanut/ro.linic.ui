package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Class for showing a repository name and location
 *
 * @since 3.4
 *
 */
public class RepositoryNameAndLocationDialog extends StatusDialog {

	Button okButton;
	Text url, nickname;
	ProvisioningUI ui;
	URI location;
	String name;
	String initialURL;

	public RepositoryNameAndLocationDialog(final Shell parentShell, final ProvisioningUI ui) {
		super(parentShell);
		this.ui = ui;
		setTitle(ProvUIMessages.RepositoryNameAndLocationDialog_Title);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.AddRepositoryDialog_addButtonLabel, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		initializeDialogUnits(comp);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginTop = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		nickname = createNameField(comp);
		url = createLocationField(comp);

		comp.setLayout(layout);
		final GridData data = new GridData();
		comp.setLayoutData(data);

		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * Return a RepositoryTracker appropriate for validating and adding the
	 * repository.
	 *
	 * @return the Repository Tracker
	 */
	protected RepositoryTracker getRepositoryTracker() {
		return ui.getRepositoryTracker();
	}

	@Override
	protected void okPressed() {
		if (handleOk())
			super.okPressed();
	}

	protected boolean handleOk() {
		final IStatus status = validateRepositoryURL(false);
		location = getUserLocation();
		name = nickname.getText().trim();
		return status.isOK();
	}

	/**
	 * Get the repository location as currently typed in by the user.  Return null if there
	 * is a problem with the URL.
	 *
	 * @return the URL currently typed in by the user.
	 */
	protected URI getUserLocation() {
		return getRepositoryTracker().locationFromString(url.getText().trim());
	}

	/**
	 * Get the location of the repository that was entered by the user.
	 * Return <code>null</code> if no location was provided.
	 *
	 * @return the location of the repository that has been provided by the user.
	 */
	public URI getLocation() {
		return location;
	}

	/**
	 * Get the name of the repository that was entered by the user.
	 * Return <code>null</code> if no name was provided.
	 *
	 * @return the name of the repository that has been provided by the user.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Validate the repository URL, returning a status that is appropriate
	 * for showing the user.  The boolean indicates whether the repositories
	 * should be consulted for validating the URL.  For example, it is not
	 * appropriate to contact the repositories on every keystroke.
	 */
	protected IStatus validateRepositoryURL(final boolean contactRepositories) {
		if (url == null || url.isDisposed())
			return Status.OK_STATUS;
		final IStatus[] status = new IStatus[1];
		status[0] = getRepositoryTracker().getInvalidLocationStatus(url.getText().trim());
		final URI userLocation = getUserLocation();
		if (url.getText().length() == 0)
			status[0] = new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, RepositoryTracker.STATUS_INVALID_REPOSITORY_LOCATION, ProvUIMessages.RepositoryGroup_URLRequired, null);
		else if (userLocation == null)
			status[0] = new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, RepositoryTracker.STATUS_INVALID_REPOSITORY_LOCATION, ProvUIMessages.AddRepositoryDialog_InvalidURL, null);
		else {
			if (initialURL.equals(url.getText().trim()))
				status[0] = Status.OK_STATUS;
			else if (userLocation.equals(getOriginalLocation()))
				// the location is reverted to the original one that has not been saved
				status[0] = Status.OK_STATUS;
			else
				BusyIndicator.showWhile(getShell().getDisplay(), () -> status[0] = getRepositoryTracker().validateRepositoryLocation(ui.getSession(), userLocation, contactRepositories, null));
		}
		// At this point the subclasses may have decided to opt out of
		// this dialog.
		if (status[0].getSeverity() == IStatus.CANCEL) {
			cancelPressed();
		}

		setOkEnablement(status[0].isOK());
		updateStatus(status[0]);
		return status[0];

	}

	@Override
	protected void updateButtonsEnableState(final IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	protected void setOkEnablement(final boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}

	protected String getInitialLocationText() {
		return "https://"; //$NON-NLS-1$
	}

	protected String getInitialNameText() {
		return ""; //$NON-NLS-1$
	}

	/**
	 * the location of repository to be changed
	 * @return the location of an existing repository
	 */
	protected URI getOriginalLocation() {
		return null;
	}

	protected Text createNameField(final Composite parent) {
		// Name: []
		final Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText(ProvUIMessages.AddRepositoryDialog_NameLabel);
		nickname = new Text(parent, SWT.BORDER);
		nickname.setText(getInitialNameText());
		final GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);

		nickname.setLayoutData(data);
		return nickname;
	}

	protected Text createLocationField(final Composite parent) {
		// Location: []
		final Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setText(ProvUIMessages.AddRepositoryDialog_LocationLabel);
		url = new Text(parent, SWT.BORDER);
		final GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		url.setLayoutData(data);
		final DropTarget target = new DropTarget(url, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
		target.addDropListener(new TextURLDropAdapter(url, true));
		url.addModifyListener(e -> validateRepositoryURL(false));
		initialURL = getInitialLocationText();
		url.setText(initialURL);
		url.setSelection(0, url.getText().length());
		return url;
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}
}
