package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

import ro.linic.ui.p2.internal.ui.viewers.DeferredQueryContentProvider;
import ro.linic.ui.p2.org.eclipse.ui.FilteredTree;
import ro.linic.ui.p2.org.eclipse.ui.PatternFilter;
import ro.linic.ui.p2.org.eclipse.ui.WorkbenchJob;

/**
 * FilteredTree extension that creates a ContainerCheckedTreeViewer, manages the
 * check state across filtering (working around bugs in ContainerCheckedTreeViewer),
 * and preloads all metadata repositories before allowing filtering, in order to
 * coordinate background fetch and filtering.  It also manages a cache of expanded
 * elements that can survive a change of input.
 *
 * @since 3.4
 *
 */
public class DelayedFilterCheckboxTree extends FilteredTree {

	private static final long FILTER_DELAY = 800;

	public static final Object ALL_ITEMS_HACK = new Object();

	ToolBar toolBar;
	Display display;
	PatternFilter patternFilter;
	IPreFilterJobProvider jobProvider;
	DeferredQueryContentProvider contentProvider;
	String savedFilterText;
	Job preFilterJob;
	WorkbenchJob filterJob;
	boolean ignoreFiltering = true;
	Object viewerInput;
	HashSet<Object> checkState = new HashSet<>();
	Set<Object> expanded = new HashSet<>();
	ContainerCheckedTreeViewer checkboxViewer;

	public DelayedFilterCheckboxTree(final Composite parent, final int treeStyle, final PatternFilter filter, final IPreFilterJobProvider jobProvider) {
		super(parent, true);
		this.display = parent.getDisplay();
		this.patternFilter = filter;
		init(treeStyle, filter);
	}

	@Override
	protected TreeViewer doCreateTreeViewer(final Composite composite, final int style) {
		checkboxViewer = new ContainerCheckedTreeViewer(composite, style);
		checkboxViewer.addCheckStateListener(event -> {
			// We use an additive check state cache so we need to remove
			// previously checked items if the user unchecked them.
			if (!event.getChecked() && checkState != null) {
				if (event.getElement() == ALL_ITEMS_HACK) {
					clearCheckStateCache();
				} else {
					final ArrayList<Object> toRemove = new ArrayList<>(1);
					// See bug 258117.  Ideally we would get check state changes
					// for children when the parent state changed, but we aren't, so
					// we need to remove all children from the additive check state
					// cache.
					if (contentProvider.hasChildren(event.getElement())) {
						final Set<Object> unchecked = new HashSet<>();
						// See bug 533655, We should uncheck all of the children
						// of the triggering element, not just the direct descendants.
						uncheckAllChildren(unchecked, event.getElement());

						final Iterator<Object> iter = checkState.iterator();
						while (iter.hasNext()) {
							final Object current = iter.next();
							if (current != null && unchecked.contains(current)) {
								toRemove.add(current);
							}
						}
					} else {
						for (final Object element2 : checkState) {
							if (checkboxViewer.getComparer().equals(element2, event.getElement())) {
								toRemove.add(element2);
								// Do not break out of the loop.  We may have duplicate equal
								// elements in the cache.  Since the cache is additive, we want
								// to be sure we've gotten everything.
							}
						}
					}
					checkState.removeAll(toRemove);
				}
			} else if (event.getChecked()) {
				rememberLeafCheckState();
			}
		});
		return checkboxViewer;
	}

	private void uncheckAllChildren(final Set<Object> unchecked, final Object element) {
		for (final Object child : contentProvider.getChildren(element)) {
			unchecked.add(child);
			if (contentProvider.getChildren(child).length > 0) {
				uncheckAllChildren(unchecked, child);
			}
		}
	}

	@Override
	protected Composite createFilterControls(final Composite filterParent) {
		super.createFilterControls(filterParent);
		filterParent.addDisposeListener(e -> cancelPreFilterJob());
		return filterParent;
	}

	public void contentProviderSet(final DeferredQueryContentProvider deferredProvider) {
		this.contentProvider = deferredProvider;
		deferredProvider.addListener((v, oldInput, newInput) -> {
			if (newInput == null) {
				return;
			}
			// Store the input because it's not reset in the viewer until
			// after this listener is run.
			viewerInput = newInput;

			// If we were loading repos, we want to cancel because there may be more.
			cancelPreFilterJob();
			// Cancel any filtering
			cancelAndResetFilterJob();
			contentProvider.setSynchronous(false);
			// Remember any previous expansions
			rememberExpansions();
			// If there are remembered check states, try to restore them.
			// Must be done in an async because we are in the middle of a buggy
			// selection preserving viewer refresh.
			checkboxViewer.getTree().setRedraw(false);
			display.asyncExec(() -> {
				if (checkboxViewer.getTree().isDisposed()) {
					return;
				}
				rememberExpansions();
				restoreLeafCheckState();
				rememberExpansions();
				restoreExpansions();
				checkboxViewer.getTree().setRedraw(true);
			});
		});
	}

	public void clearCheckStateCache() {
		checkState = null;
	}

	/*
	 * Overridden to hook a listener on the job and set the deferred content provider
	 * to synchronous mode before a filter is done.
	 * @see org.eclipse.ui.dialogs.FilteredTree#doCreateRefreshJob()
	 */
	@Override
	protected WorkbenchJob doCreateRefreshJob() {
		filterJob = super.doCreateRefreshJob();

		filterJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void aboutToRun(final IJobChangeEvent event) {
				// If we know we've already filtered and loaded repos, nothing more to do
				if (!ignoreFiltering) {
					return;
				}
				final boolean[] shouldPreFilter = new boolean[1];
				shouldPreFilter[0] = false;
				display.syncExec(() -> {
					if (filterText != null && !filterText.isDisposed()) {
						final String text = getFilterString();
						// If we are about to filter and there is
						// actually filtering to do, check for a prefilter
						// job and the content  provider to synchronous mode.
						// We want the prefilter job to complete before continuing with filtering.
						if (text == null || (initialText != null && initialText.equals(text))) {
							return;
						}
						if (!contentProvider.getSynchronous() && preFilterJob == null) {
							if (filterText != null && !filterText.isDisposed()) {
								shouldPreFilter[0] = true;
							}
						}
					}
				});
				if (shouldPreFilter[0]) {
					event.getJob().sleep();
					schedulePreFilterJob();
				} else if (ignoreFiltering) {
					event.getJob().sleep();
				} else {
					// shouldn't get here unless the prefilter job finished
					// and ignoreFiltering became false since we entered this listener.
					rememberLeafCheckState();
				}
			}

			@Override
			public void running(final IJobChangeEvent event) {
				display.syncExec(() -> rememberLeafCheckState());
			}

			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					display.asyncExec(() -> {
						if (checkboxViewer.getTree().isDisposed()) {
							return;
						}

						checkboxViewer.getTree().setRedraw(false);
						// remember things expanded by the filter
						rememberExpansions();
						restoreLeafCheckState();
						// now restore expansions because we may have
						// had others
						restoreExpansions();
						checkboxViewer.getTree().setRedraw(true);
					});
				}
			}
		});
		return filterJob;
	}

	void schedulePreFilterJob() {
		// cancel any existing jobs
		cancelPreFilterJob();
		ignoreFiltering = false;
		preFilterJob = jobProvider == null ? null : jobProvider.getPreFilterJob();
		if (preFilterJob == null) {
			if (filterJob != null) {
				filterJob.wakeUp();
			}
			return;
		}
		ignoreFiltering = true;
		preFilterJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				ignoreFiltering = false;
				contentProvider.setSynchronous(true);
				if (filterJob != null) {
					filterJob.wakeUp();
				}
				preFilterJob = null;
			}
		});
		preFilterJob.setSystem(true);
		preFilterJob.setUser(false);
		preFilterJob.schedule();
	}

	void cancelPreFilterJob() {
		if (preFilterJob != null) {
			preFilterJob.cancel();
			preFilterJob = null;
		}
	}

	void cancelAndResetFilterJob() {
		if (filterJob != null) {
			filterJob.cancel();
		}
	}

	void rememberLeafCheckState() {
		final ContainerCheckedTreeViewer v = (ContainerCheckedTreeViewer) getViewer();
		final Object[] checked = v.getCheckedElements();
		if (checkState == null) {
			checkState = new HashSet<>(checked.length);
		}
		for (final Object element : checked) {
			if (!v.getGrayed(element) && contentProvider.getChildren(element).length == 0) {
				if (!checkState.contains(element)) {
					checkState.add(element);
				}
			}
		}
	}

	void restoreLeafCheckState() {
		if (checkboxViewer == null || checkboxViewer.getTree().isDisposed()) {
			return;
		}
		if (checkState == null) {
			return;
		}

		checkboxViewer.setCheckedElements(new Object[0]);
		checkboxViewer.setGrayedElements(new Object[0]);
		// Now we are only going to set the check state of the leaf nodes
		// and rely on our container checked code to update the parents properly.
		final Iterator<Object> iter = checkState.iterator();
		Object element = null;
		if (iter.hasNext()) {
			checkboxViewer.expandAll();
		}
		while (iter.hasNext()) {
			element = iter.next();
			checkboxViewer.setChecked(element, true);
		}
		// We are only firing one event, knowing that this is enough for our listeners.
		if (element != null) {
			checkboxViewer.fireCheckStateChanged(element, true);
		}
	}

	void rememberExpansions() {
		// The expansions are additive, but we are using a set to keep out
		// duplicates.  In practice, this means expanded items from different
		// inputs will remain expanded, such as categories with the same name
		// in different repos.
		expanded.addAll(Arrays.asList(getViewer().getExpandedElements()));
	}

	void restoreExpansions() {
		getViewer().setExpandedElements(expanded.toArray());
	}

	public ContainerCheckedTreeViewer getCheckboxTreeViewer() {
		return checkboxViewer;
	}

	@Override
	protected long getRefreshJobDelay() {
		return FILTER_DELAY;
	}

	public Object[] getCheckedElements() {
		if (this.checkState != null) {
			return this.checkState.toArray();
		}
		return new Object[0];
	}

	@Override
	protected String getFilterString() {
		return super.getFilterString();
	}
}
