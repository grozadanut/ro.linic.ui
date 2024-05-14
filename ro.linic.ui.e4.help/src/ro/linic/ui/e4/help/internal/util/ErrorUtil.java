package ro.linic.ui.e4.help.internal.util;

import org.eclipse.help.internal.base.util.IErrorUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.e4.help.Messages;

/**
 * Utility class for common error displaying tasks.
 */
public class ErrorUtil implements IErrorUtil {

	@Override
	public void displayError(final String msg) {
		displayErrorDialog(msg);
	}

	@Override
	public void displayError(final String msg, final Thread uiThread) {
		try {
			Display.findDisplay(uiThread).asyncExec(() -> displayErrorDialog(msg));
		} catch (final Exception e2) {
		}
	}

	/**
	 * Immediately displays error dialog with a given string
	 *
	 * @param msg
	 *            error message to display and log.
	 */
	public static void displayErrorDialog(final String msg) {
		final String title = Messages.Help_Error;
		Shell shell;
		if (Display.getCurrent() != null) {
			shell = Display.getCurrent().getActiveShell();
		} else {
			shell = new Shell();
		}
		MessageDialog.openError(shell, title, msg);
	}

	/**
	 * Immediately displays an Information dialog with a given string
	 *
	 * @param msg
	 *            error message to display.
	 */
	public static void displayInfoDialog(final String msg) {
		final String title = Messages.Help_Info;
		Shell shell;
		if (Display.getCurrent() != null) {
			shell = Display.getCurrent().getActiveShell();
		} else {
			shell = new Shell();
		}
		MessageDialog.openInformation(shell, title, msg);
	}

	/**
	 * Immediately displays a Question dialog with a given string (question).
	 *
	 * @return which button(Yes/No) was pressed by user
	 */
	public static boolean displayQuestionDialog(final String msg) {
		final String title = Messages.Help_Question;
		Shell shell;
		if (Display.getCurrent() != null) {
			shell = Display.getCurrent().getActiveShell();
		} else {
			shell = new Shell();
		}
		return MessageDialog.openQuestion(shell, title, msg);
	}
}
