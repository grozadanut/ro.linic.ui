package ro.linic.ui.p2.ui;

import org.eclipse.swt.widgets.Control;

/**
 * ICopyable defines an interface for elements that provide
 * copy support in a UI.  The active control in the UI determines
 * what should be copied.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICopyable {
	/**
	 * Copy text related to the active control to the clipboard.
	 *
	 * @param activeControl the active control
	 */
	public void copyToClipboard(Control activeControl);
}
