package ro.linic.ui.p2.org.eclipse.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The PendingUpdateAdapter is a convenience object that can be used by a
 * BaseWorkbenchContentProvider that wants to show a pending update.
 *
 * @since 3.2
 */
public class PendingUpdateAdapter implements IWorkbenchAdapter, IAdaptable {

	private boolean removed = false;

	/**
	 * Return whether or not this has been removed from the tree.
	 *
	 * @return boolean
	 */
	protected boolean isRemoved() {
		return removed;
	}

	/**
	 * Set whether or not this has been removed from the tree.
	 *
	 * @param removedValue boolean
	 */
	protected void setRemoved(final boolean removedValue) {
		this.removed = removedValue;
	}

	/**
	 * Create a new instance of the receiver.
	 */
	public PendingUpdateAdapter() {
		// No initial behavior
	}

	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		return null;
	}

	@Override
	public Object[] getChildren(final Object o) {
		return new Object[0];
	}

	@Override
	public ImageDescriptor getImageDescriptor(final Object object) {
		return null;
	}

	@Override
	public String getLabel(final Object o) {
		return "Pending...";
	}

	@Override
	public Object getParent(final Object o) {
		return null;
	}

	/**
	 * @since 3.4
	 */
	@Override
	public String toString() {
		return getLabel(null);
	}
}
