package ro.linic.ui.p2.org.eclipse.ui;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * The DeferredContentManager is a class that helps an ITreeContentProvider get
 * its deferred input.
 *
 * <b>NOTE</b> AbstractTreeViewer#isExpandable may need to be implemented in
 * AbstractTreeViewer subclasses with deferred content that use filtering as a
 * call to #getChildren may be required to determine the correct state of the
 * expanding control.
 *
 * AbstractTreeViewers which use this class may wish to sacrifice accuracy of
 * the expandable state indicator for the performance benefits of deferring
 * content.
 *
 * @since 3.0
 */
public class DeferredTreeContentManager {

	AbstractTreeViewer treeViewer;

	private ListenerList<IJobChangeListener> updateCompleteListenerList;

	/**
	 * The DeferredContentFamily is a class used to keep track of a manager-object
	 * pair so that only jobs scheduled by the receiver are canceled by the
	 * receiver.
	 *
	 * @since 3.1
	 *
	 */
	static class DeferredContentFamily {
		protected DeferredTreeContentManager manager;
		protected Object element;

		/**
		 * Create a new instance of the receiver to define a family for object in a
		 * particular scheduling manager.
		 *
		 * @param schedulingManager
		 * @param object
		 */
		DeferredContentFamily(final DeferredTreeContentManager schedulingManager, final Object object) {
			this.manager = schedulingManager;
			this.element = object;
		}
	}

	/**
	 * Create a new instance of the receiver using the supplied content provider and
	 * viewer.
	 *
	 * @param viewer The tree viewer that the results are added to
	 *
	 * @since 3.4
	 */
	public DeferredTreeContentManager(final AbstractTreeViewer viewer) {
		treeViewer = viewer;
	}

	/**
	 * Provides an optimized lookup for determining if an element has children. This
	 * is required because elements that are populated lazilly can't answer
	 * <code>getChildren</code> just to determine the potential for children. Throw
	 * an AssertionFailedException if element is null.
	 *
	 * @param element The Object being tested. This should not be <code>null</code>.
	 * @return boolean <code>true</code> if there are potentially children.
	 * @throws RuntimeException if the element is null.
	 */
	public boolean mayHaveChildren(final Object element) {
		Assert.isNotNull(element, "Not an IDeferredWorkbenchAdapter");
		final IDeferredWorkbenchAdapter adapter = getAdapter(element);
		return adapter != null && adapter.isContainer();
	}

	/**
	 * Returns the child elements of the given element, or in the case of a deferred
	 * element, returns a placeholder. If a deferred element is used, a job is
	 * created to fetch the children in the background.
	 *
	 * @param parent The parent object.
	 * @return Object[] or <code>null</code> if parent is not an instance of
	 *         IDeferredWorkbenchAdapter.
	 */
	public Object[] getChildren(final Object parent) {
		final IDeferredWorkbenchAdapter element = getAdapter(parent);
		if (element == null) {
			return null;
		}
		final PendingUpdateAdapter placeholder = createPendingUpdateAdapter();
		startFetchingDeferredChildren(parent, element, placeholder);
		return new Object[] { placeholder };
	}

	/**
	 * Factory method for creating the pending update adapter representing the
	 * placeholder node. Subclasses may override.
	 *
	 * @return a pending update adapter
	 * @since 3.2
	 */
	protected PendingUpdateAdapter createPendingUpdateAdapter() {
		return new PendingUpdateAdapter();
	}

	/**
	 * Return the IDeferredWorkbenchAdapter for element or the element if it is an
	 * instance of IDeferredWorkbenchAdapter. If it does not exist return null.
	 *
	 * @param element element to adapt
	 * @return IDeferredWorkbenchAdapter or <code>null</code>
	 */
	protected IDeferredWorkbenchAdapter getAdapter(final Object element) {
		return Adapters.adapt(element, IDeferredWorkbenchAdapter.class);
	}

	/**
	 * Starts a job and creates a collector for fetching the children of this
	 * deferred adapter. If children are waiting to be retrieved for this parent
	 * already, that job is cancelled and another is started.
	 *
	 * @param parent      The parent object being filled in,
	 * @param adapter     The adapter being used to fetch the children.
	 * @param placeholder The adapter that will be used to indicate that results are
	 *                    pending.
	 */
	protected void startFetchingDeferredChildren(final Object parent, final IDeferredWorkbenchAdapter adapter,
			final PendingUpdateAdapter placeholder) {
		final IElementCollector collector = createElementCollector(parent, placeholder);
		// Cancel any jobs currently fetching children for the same parent
		// instance.
		cancel(parent);
		final String jobName = getFetchJobName(parent, adapter);
		final Job job = new Job(jobName) {
			@Override
			public IStatus run(final IProgressMonitor monitor) {
				adapter.fetchDeferredChildren(parent, collector, monitor);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(final Object family) {
				if (family instanceof DeferredContentFamily) {
					final DeferredContentFamily contentFamily = (DeferredContentFamily) family;
					if (contentFamily.manager == DeferredTreeContentManager.this) {
						return isParent(contentFamily, parent);
					}
				}
				return false;

			}

			/**
			 * Check if the parent of element is equal to the parent used in this job.
			 *
			 * @param family The DeferredContentFamily that defines a potential ancestor of
			 *               the current parent in a particular manager.
			 * @param child  The object to check against.
			 * @return boolean <code>true</code> if the child or one of its parents are the
			 *         same as the element of the family.
			 */
			private boolean isParent(final DeferredContentFamily family, final Object child) {
				if (family.element.equals(child)) {
					return true;
				}
				final IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter(child);
				if (workbenchAdapter == null) {
					return false;
				}
				final Object elementParent = workbenchAdapter.getParent(child);
				if (elementParent == null) {
					return false;
				}
				return isParent(family, elementParent);
			}

			/**
			 * Get the workbench adapter for the element.
			 *
			 * @param element The object we are adapting to.
			 */
			private IWorkbenchAdapter getWorkbenchAdapter(final Object element) {
				return Adapters.adapt(element, IWorkbenchAdapter.class);
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				runClearPlaceholderJob(placeholder);
			}
		});
		job.setRule(adapter.getRule(parent));
		job.schedule();
	}

	/**
	 * Returns a name to use for the job that fetches children of the given parent.
	 * Subclasses may override. Default job name is parent's label.
	 *
	 * @param parent  parent that children are to be fetched for
	 * @param adapter parent's deferred adapter
	 * @return job name
	 */
	protected String getFetchJobName(final Object parent, final IDeferredWorkbenchAdapter adapter) {
		return NLS.bind("Fetching children of {0}", adapter.getLabel(parent));
	}

	/**
	 * Create a UIJob to add the children to the parent in the tree viewer.
	 *
	 * @param parent   the parent object being filled in
	 * @param children the elements being added
	 * @param monitor  a progress monitor
	 */
	protected void addChildren(final Object parent, final Object[] children, final IProgressMonitor monitor) {
		final WorkbenchJob updateJob = new WorkbenchJob("Adding children") {
			@Override
			public IStatus runInUIThread(final IProgressMonitor updateMonitor) {
				// Cancel the job if the tree viewer got closed
				if (treeViewer.getControl().isDisposed() || updateMonitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				treeViewer.add(parent, children);
				return Status.OK_STATUS;
			}
		};
		updateJob.setSystem(true);
		updateJob.schedule();

	}

	/**
	 * Return whether or not the element is or adapts to an
	 * IDeferredWorkbenchAdapter.
	 *
	 * @param element the element to test
	 * @return boolean <code>true</code> if the element is an
	 *         IDeferredWorkbenchAdapter
	 */
	public boolean isDeferredAdapter(final Object element) {
		return getAdapter(element) != null;
	}

	/**
	 * Run a job to clear the placeholder. This is used when the update for the tree
	 * is complete so that the user is aware that no more updates are pending.
	 *
	 * @param placeholder placeholder to clear
	 */
	protected void runClearPlaceholderJob(final PendingUpdateAdapter placeholder) {
		if (placeholder.isRemoved() || Display.getCurrent() == null) {
			return;
		}
		// Clear the placeholder if it is still there
		final WorkbenchJob clearJob = new WorkbenchJob("Clear") {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				if (!placeholder.isRemoved()) {
					final Control control = treeViewer.getControl();
					if (control.isDisposed()) {
						return Status.CANCEL_STATUS;
					}
					treeViewer.remove(placeholder);
					placeholder.setRemoved(true);
				}
				return Status.OK_STATUS;
			}
		};
		clearJob.setSystem(true);

		if (updateCompleteListenerList != null) {
			for (final IJobChangeListener listener : updateCompleteListenerList) {
				clearJob.addJobChangeListener(listener);
			}
		}
		// See bug 470554 if IElementCollector.done() is called immediately
		// after IElementCollector.add(), SWT/GTK seem to be confused.
		// Delay tree element deletion to avoid race conditions with GTK code
		final long timeout = Util.isGtk() ? 100 : 0;
		clearJob.schedule(timeout);
	}

	/**
	 * Cancel all jobs that are fetching content for the given parent or any of its
	 * children.
	 *
	 * @param parent the root to cancel
	 */
	public void cancel(final Object parent) {
		if (parent == null) {
			return;
		}

		Job.getJobManager().cancel(new DeferredContentFamily(this, parent));
	}

	/**
	 * Create the element collector for the receiver.
	 *
	 * @param parent      The parent object being filled in,
	 * @param placeholder The adapter that will be used to indicate that results are
	 *                    pending.
	 * @return IElementCollector
	 */
	protected IElementCollector createElementCollector(final Object parent, final PendingUpdateAdapter placeholder) {
		return new IElementCollector() {
			@Override
			public void add(final Object element, final IProgressMonitor monitor) {
				add(new Object[] { element }, monitor);
			}

			@Override
			public void add(final Object[] elements, final IProgressMonitor monitor) {
				addChildren(parent, elements, monitor);
			}

			@Override
			public void done() {
				runClearPlaceholderJob(placeholder);
			}
		};
	}

	/**
	 * Add a listener to list of update complete listeners. These listeners are
	 * attached to the job that updates the viewer content (clears the pending
	 * entry, etc.) after all deferred content has been retrieved.
	 *
	 * This method has no effect if the listener has already been added to the list
	 * of listeners.
	 *
	 * Since 3.6, this listener is added to a list of listeners rather than
	 * replacing the previously added listener. For backward compatibility, adding a
	 * null listener will be interpreted as removal of a listener if only one
	 * listener has been registered.
	 *
	 * @param listener the listener to add to the list of update listeners
	 * @since 3.4
	 */
	public void addUpdateCompleteListener(final IJobChangeListener listener) {
		// Maintain backward compatibility.
		// Earlier only one listener was supported, so it can be removed by
		// passing null
		if (listener == null && updateCompleteListenerList != null) {
			final Object[] listeners = updateCompleteListenerList.getListeners();
			if (listeners.length == 1) {
				removeUpdateCompleteListener((IJobChangeListener) listeners[0]);
			}
		} else {
			if (updateCompleteListenerList == null) {
				updateCompleteListenerList = new ListenerList<>();
			}
			updateCompleteListenerList.add(listener);
		}
	}

	/**
	 * Removes the listener from the list of update listeners that are attached to
	 * the job that updates the viewer content (clears the pending entry, etc.)
	 * after all deferred content has been retrieved. If the listener is already
	 * attached to a running job, it is not removed, but it will not be added to any
	 * subsequent jobs that are run.
	 *
	 * This method has no effect if the listener was not previously added to the
	 * listener list.
	 *
	 * @param listener the listener to be removed
	 * @since 3.6
	 */
	public void removeUpdateCompleteListener(final IJobChangeListener listener) {
		if (updateCompleteListenerList != null) {
			updateCompleteListenerList.remove(listener);
		}
	}

}
