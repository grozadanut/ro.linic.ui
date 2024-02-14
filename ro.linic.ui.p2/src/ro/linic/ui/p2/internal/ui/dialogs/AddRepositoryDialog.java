package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Abstract dialog class for adding repositories of different types. This class
 * assumes the user view of a repository is a name and URI. Individual subclasses
 * will dictate what kind of repository and how it's created.
 *
 * @since 3.4
 *
 */
public abstract class AddRepositoryDialog extends RepositoryNameAndLocationDialog {

	URI addedLocation;
	static final String[] ARCHIVE_EXTENSIONS = new String[] {"*.jar;*.zip"}; //$NON-NLS-1$
	static String lastLocalLocation = null;
	static String lastArchiveLocation = null;
	Policy policy;

	public AddRepositoryDialog(final Shell parentShell, final ProvisioningUI ui) {
		super(parentShell, ui);
		setTitle(ProvUIMessages.AddRepositoryDialog_Title);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IProvHelpContextIds.ADD_REPOSITORY_DIALOG);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		initializeDialogUnits(comp);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginTop = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		comp.setLayout(layout);
		final GridData data = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(data);

		// Name: []
		nickname = createNameField(comp);

		final Button localButton = new Button(comp, SWT.PUSH);
		localButton.setText(ProvUIMessages.RepositoryGroup_LocalRepoBrowseButton);
		localButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL);
			dialog.setMessage(ProvUIMessages.RepositoryGroup_SelectRepositoryDirectory);
			dialog.setFilterPath(lastLocalLocation);
			final String path = dialog.open();
			if (path != null) {
				lastLocalLocation = path;
				url.setText(makeLocalURIString(path));
				validateRepositoryURL(false);
			}
		}));
		setButtonLayoutData(localButton);

		// Location: []
		url = createLocationField(comp);

		final Button archiveButton = new Button(comp, SWT.PUSH);
		archiveButton.setText(ProvUIMessages.RepositoryGroup_ArchivedRepoBrowseButton);
		archiveButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final FileDialog dialog = new FileDialog(getShell(), SWT.APPLICATION_MODAL);
			dialog.setText(ProvUIMessages.RepositoryGroup_RepositoryFile);
			dialog.setFilterExtensions(ARCHIVE_EXTENSIONS);
			dialog.setFileName(lastArchiveLocation);
			final String path = dialog.open();
			if (path != null) {
				lastArchiveLocation = path;
				url.setText(makeLocalURIString(path));
				validateRepositoryURL(false);
			}
		}));
		setButtonLayoutData(archiveButton);
		comp.setTabList(new Control[] {nickname, url, localButton, archiveButton});
		Dialog.applyDialogFont(comp);
		return comp;
	}

	String makeLocalURIString(final String path) {
		try {
			final URI localURI = URIUtil.fromString(path);
			return URIUtil.toUnencodedString(RepositoryHelper.localRepoURIHelper(localURI));
		} catch (final URISyntaxException e) {
			return path;
		}
	}

	@Override
	protected boolean handleOk() {
		final IStatus status = addRepository();
		return status.isOK();
	}

	/**
	 * Get the location of the repository that was added by this dialog.  Return <code>null</code>
	 * if the dialog has not yet added a repository location.
	 *
	 * @return the location of the repository that has been added by this dialog, or <code>null</code>
	 * if no repository has been added.
	 */
	public URI getAddedLocation() {
		return addedLocation;
	}

	protected IStatus addRepository() {
		final IStatus status = validateRepositoryURL(false);
		if (status.isOK()) {
			addedLocation = getUserLocation();
			String nick = nickname.getText().trim();
			if (nick.length() == 0)
				nick = null;
			getRepositoryTracker().addRepository(addedLocation, nick, getProvisioningUI().getSession());
		}
		return status;
	}
	@Override
	protected boolean isResizable() {
		return true;
	}
}
