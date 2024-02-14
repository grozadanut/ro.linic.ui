package ro.linic.ui.p2.internal.ui.statushandlers;

import org.eclipse.jface.dialogs.Dialog;

public class StatusManager {
	/**
	 * A style indicating that the status should not be acted on. This is used by
	 * objects such as log listeners that do not want to report a status twice.
	 */
	public static final int NONE = 0;

	/**
	 * A style indicating that the status should be logged only.
	 */
	public static final int LOG = 0x01;

	/**
	 * A style indicating that handlers should show a problem to an user without
	 * blocking the calling method while awaiting user response. This is generally
	 * done using a non modal {@link Dialog}.
	 */
	public static final int SHOW = 0x02;

	/**
	 * A style indicating that the handling should block the calling thread until
	 * the status has been handled.
	 * <p>
	 * A typical usage of this would be to ensure that the user's actions are
	 * blocked until they've dealt with the status in some manner. It is therefore
	 * likely but not required that the <code>StatusHandler</code> would achieve
	 * this through the use of a modal dialog.
	 * </p>
	 * <p>
	 * Due to the fact that use of <code>BLOCK</code> will block UI, care should be
	 * taken in this use of this flag.
	 * </p>
	 */
	public static final int BLOCK = 0x04;
}
