package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import ro.linic.ui.p2.internal.ui.model.QueriedElement;

/**
 * Copy of ContainerCheckedTreeViewer which is specialized for use with
 * DeferredFetchFilteredTree. Originally copied from org.eclipse.ui.dialogs and
 * altered to achieve the following:
 *
 * (1)checking a parent will expand it when we know it's a long running
 * operation that involves a placeholder. The modified method is
 * doCheckStateChanged().
 *
 * (2)when preserving selection, we do not want the check state to be rippled
 * through the child and parent nodes. Since we know that
 * preservingSelection(Runnable) isn't working properly, no need to do a bunch
 * of work here. The added methods is preservingSelection(Runnable). Modified
 * methods are updateChildrenItems(TreeItem parent) and
 * updateParentItems(TreeItem parent).
 *
 * (3)we correct the problem with preservingSelection(Runnable) by remembering
 * the check state and restoring it after a refresh. We fire a check state event
 * so clients monitoring the selection will know what's going on. Added methods
 * are internalRefresh(Object, boolean), saveCheckedState(),
 * restoreCheckedState(), and fireCheckStateChanged(Object, boolean). That last
 * method is public so that DeferredFetchFilteredTree can do the same thing when
 * it remembers selections.
 *
 * This class does not correct the general problem reported in
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=170521 That is handled by
 * preserving selections additively in DeferredFetchFilteredTree. This class
 * simply provides the API needed by that class and manages the parent state
 * according to the children.
 */
public class ContainerCheckedTreeViewer extends CheckboxTreeViewer {

	private boolean rippleCheckMarks = true;
	private ArrayList<Object> savedCheckState;

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 *
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Composite)
	 */
	public ContainerCheckedTreeViewer(final Composite parent) {
		super(parent);
		initViewer();
	}

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 *
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Composite,int)
	 */
	public ContainerCheckedTreeViewer(final Composite parent, final int style) {
		super(parent, style);
		initViewer();
	}

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 *
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Tree)
	 */
	public ContainerCheckedTreeViewer(final Tree tree) {
		super(tree);
		initViewer();
	}

	private void initViewer() {
		setUseHashlookup(true);
		addCheckStateListener(event -> doCheckStateChanged(event.getElement()));
		addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {
			}

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {
				final Widget item = findItem(event.getElement());
				if (item instanceof TreeItem) {
					initializeItem((TreeItem) item);
				}
			}
		});
	}

	/**
	 * Update element after a checkstate change.
	 *
	 * @param element
	 */
	protected void doCheckStateChanged(final Object element) {
		final Widget item = findItem(element);
		if (item instanceof TreeItem) {
			final TreeItem treeItem = (TreeItem) item;
			treeItem.setGrayed(false);
			// BEGIN MODIFICATION OF COPIED CLASS
			if (element instanceof QueriedElement && treeItem.getChecked()) {
				if (!((QueriedElement) element).hasQueryable()) {
					// We have checked an element that will take some time
					// to get its children. Use this opportunity to auto-expand
					// the tree so that the check mark is not misleading. Don't
					// update the check state because it will just be a pending
					// placeholder.
					expandToLevel(element, 1);
					return;
				}
			}
			// END MODIFICATION OF COPIED CLASS
			updateChildrenItems(treeItem);
			updateParentItems(treeItem.getParentItem());
		}
	}

	/**
	 * The item has expanded. Updates the checked state of its children.
	 */
	private void initializeItem(final TreeItem item) {
		if (item.getChecked() && !item.getGrayed()) {
			updateChildrenItems(item);
		}
	}

	/**
	 * Updates the check state of all created children
	 */
	// MODIFIED to ignore parent state when in the middle of a
	// selection preserving refresh.
	private void updateChildrenItems(final TreeItem parent) {
		// We are in the middle of preserving selections, don't
		// update any children according to parent
		if (!rippleCheckMarks)
			return;
		final Item[] children = getChildren(parent);
		final boolean state = parent.getChecked();
		for (final Item element : children) {
			final TreeItem curr = (TreeItem) element;
			if (curr.getData() != null && ((curr.getChecked() != state) || curr.getGrayed())) {
				curr.setChecked(state);
				curr.setGrayed(false);
				updateChildrenItems(curr);
			}
		}
	}

	/**
	 * Updates the check / gray state of all parent items
	 */
	private void updateParentItems(final TreeItem item) {
		// We are in the middle of preserving selections, don't
		// update any parents according to children
		if (!rippleCheckMarks)
			return;
		if (item != null) {
			final Item[] children = getChildren(item);
			boolean containsChecked = false;
			boolean containsUnchecked = false;
			for (final Item element : children) {
				final TreeItem curr = (TreeItem) element;
				containsChecked |= curr.getChecked();
				containsUnchecked |= (!curr.getChecked() || curr.getGrayed());
			}
			item.setChecked(containsChecked);
			item.setGrayed(containsChecked && containsUnchecked);
			updateParentItems(item.getParentItem());
		}
	}

	@Override
	public boolean setChecked(final Object element, final boolean state) {
		if (super.setChecked(element, state)) {
			doCheckStateChanged(element);
			return true;
		}
		return false;
	}

	@Override
	public void setCheckedElements(final Object[] elements) {
		super.setCheckedElements(elements);
		for (final Object element : elements) {
			doCheckStateChanged(element);
		}
	}

	@Override
	protected void setExpanded(final Item item, final boolean expand) {
		super.setExpanded(item, expand);
		if (expand && item instanceof TreeItem) {
			initializeItem((TreeItem) item);
		}
	}

	@Override
	public Object[] getCheckedElements() {
		final Object[] checked = super.getCheckedElements();
		// add all items that are children of a checked node but not created yet
		final ArrayList<Object> result = new ArrayList<>();
		for (final Object curr : checked) {
			result.add(curr);
			final Widget item = findItem(curr);
			if (item != null) {
				final Item[] children = getChildren(item);
				// check if contains the dummy node
				if (children.length == 1 && children[0].getData() == null) {
					// not yet created
					collectChildren(curr, result);
				}
			}
		}
		return result.toArray();
	}

	/**
	 * Recursively add the filtered children of element to the result.
	 *
	 * @param element
	 * @param result
	 */
	private void collectChildren(final Object element, final ArrayList<Object> result) {
		final Object[] filteredChildren = getFilteredChildren(element);
		for (final Object curr : filteredChildren) {
			result.add(curr);
			collectChildren(curr, result);
		}
	}

	// The super implementation doesn't really work because the
	// non-expanded items are not holding their real elements yet.
	// Yet the code that records the checked state uses the
	// elements to remember what checkmarks should be restored.
	// The result is that non-expanded elements are not up to date
	// and if anything in there should have been checked, it
	// won't be. The best we can do is at least turn off all the
	// rippling checks that happen during this method since we are going
	// to reset all the checkmarks anyway.
	@Override
	protected void preservingSelection(final Runnable updateCode) {
		rippleCheckMarks = false;
		super.preservingSelection(updateCode);
		rippleCheckMarks = true;
	}

	@Override
	protected void internalRefresh(final Object element, final boolean updateLabels) {
		saveCheckedState();
		super.internalRefresh(element, updateLabels);
		restoreCheckedState();
	}

	// We only remember the leaves. This is specific to our
	// use case, not necessarily a good idea for fixing the general
	// problem.
	private void saveCheckedState() {
		final Object[] checked = getCheckedElements();
		savedCheckState = new ArrayList<>(checked.length);
		for (final Object element : checked)
			if (!isExpandable(element) && !getGrayed(element))
				savedCheckState.add(element);
	}

	// Now we restore checked state.
	private void restoreCheckedState() {
		setCheckedElements(new Object[0]);
		setGrayedElements(new Object[0]);
		Object element = null;
		// We are assuming that once a leaf, always a leaf.
		final Iterator<Object> iter = savedCheckState.iterator();
		while (iter.hasNext()) {
			element = iter.next();
			setChecked(element, true);
		}
		// Listeners need to know something changed.
		if (element != null)
			fireCheckStateChanged(element, true);
	}

	// This method is public so that the DeferredFetchFilteredTree can also
	// call it.
	public void fireCheckStateChanged(final Object element, final boolean state) {
		fireCheckStateChanged(new CheckStateChangedEvent(this, element, state));
	}
}
