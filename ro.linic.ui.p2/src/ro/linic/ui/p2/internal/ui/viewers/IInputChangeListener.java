package ro.linic.ui.p2.internal.ui.viewers;

import java.util.EventListener;

import org.eclipse.jface.viewers.Viewer;

/**
 * A listening interface used to signal clients when input changes
 * in a viewer.
 *
 * @since 3.4
 *
 */
public interface IInputChangeListener extends EventListener {
	public void inputChanged(Viewer v, Object oldInput, Object newInput);
}
