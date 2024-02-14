package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;

/**
 * A dialog which prompts the user to restart.
 *
 * @since 3.4
 */
public class ApplyProfileChangesDialog extends MessageDialog {
	public static final int PROFILE_IGNORE = 0;
	public static final int PROFILE_APPLYCHANGES = 1;
	public static final int PROFILE_RESTART = 2;
	private final static String[] yesNo = new String[] { ProvUIMessages.ApplyProfileChangesDialog_Restart,
			IDialogConstants.NO_LABEL };
	private final static String[] yesNoApply = new String[] { ProvUIMessages.ApplyProfileChangesDialog_Restart,
			ProvUIMessages.ApplyProfileChangesDialog_NotYet, ProvUIMessages.ApplyProfileChangesDialog_ApplyChanges };

	private int returnCode = PROFILE_IGNORE;

	private ApplyProfileChangesDialog(final Shell parent, final String title, final String message, final boolean mustRestart) {
		super(parent, title, null, // accept the default window icon
				message, NONE, mustRestart ? yesNo : yesNoApply, 0); // yes is the default
	}

	/**
	 * Prompt the user for restart or apply profile changes.
	 *
	 * @param parent      the parent shell of the dialog, or <code>null</code> if
	 *                    none
	 * @param mustRestart indicates whether the user must restart to get the
	 *                    changes. If <code>false</code>, then the user may choose
	 *                    to apply the changes to the running profile rather than
	 *                    restarting.
	 * @return one of PROFILE_IGNORE (do nothing), PROFILE_APPLYCHANGES (attempt to
	 *         apply the changes), or PROFILE_RESTART (restart the system).
	 */
	public static int promptForRestart(final Shell parent, final boolean mustRestart) {
		final String title = ProvUIMessages.PlatformUpdateTitle;
		final IProduct product = Platform.getProduct();
		final String productName = product != null && product.getName() != null ? product.getName()
				: ProvUIMessages.ApplicationInRestartDialog;
		final String message = NLS.bind(
				mustRestart ? ProvUIMessages.PlatformRestartMessage : ProvUIMessages.OptionalPlatformRestartMessage,
				productName);
		final ApplyProfileChangesDialog dialog = new ApplyProfileChangesDialog(parent, title, message, mustRestart);
		if (dialog.open() == Window.CANCEL)
			return PROFILE_IGNORE;
		return dialog.returnCode;
	}

	/**
	 * When a button is pressed, store the return code.
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(final int id) {
		switch (id) {
		case 0:
			// YES
			returnCode = PROFILE_RESTART;
			break;
		case 1:
			// NO
			returnCode = PROFILE_IGNORE;
			break;
		default:
			returnCode = PROFILE_APPLYCHANGES;
			break;
		}

		super.buttonPressed(id);
	}
}
