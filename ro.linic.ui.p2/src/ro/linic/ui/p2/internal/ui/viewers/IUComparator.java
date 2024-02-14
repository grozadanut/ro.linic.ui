package ro.linic.ui.p2.internal.ui.viewers;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import ro.linic.ui.p2.internal.ui.ProvUI;

public class IUComparator extends ViewerComparator {
	public static final int IU_NAME = 0;
	public static final int IU_ID = 1;
	private int key;
	private boolean showingId = false;

	public IUComparator(final int sortKey) {
		this.key = sortKey;
		showingId = sortKey == IU_ID;
	}

	/**
	 * Use the specified column config to determine whether the id should be used in
	 * lieu of an empty name when sorting.
	 *
	 * @param columnConfigs
	 */
	public void useColumnConfig(final IUColumnConfig[] columnConfigs) {
		for (final IUColumnConfig columnConfig : columnConfigs) {
			if (columnConfig.getColumnType() == IUColumnConfig.COLUMN_ID) {
				showingId = true;
				break;
			}
		}
	}

	@Override
	public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
		final IInstallableUnit iu1 = ProvUI.getAdapter(obj1, IInstallableUnit.class);
		final IInstallableUnit iu2 = ProvUI.getAdapter(obj2, IInstallableUnit.class);
		if (iu1 == null || iu2 == null)
			// If these are not iu's use the super class comparator.
			return super.compare(viewer, obj1, obj2);

		String key1, key2;
		if (key == IU_NAME) {
			// Compare the iu names in the default locale.
			// If a name is not defined, we use blank if we know the id is shown in another
			// column. If the id is not shown elsewhere, then we are displaying it, so use
			// the id instead.
			key1 = iu1.getProperty(IInstallableUnit.PROP_NAME, null);
			if (key1 == null)
				if (showingId)
					key1 = ""; //$NON-NLS-1$
				else
					key1 = iu1.getId();
			key2 = iu2.getProperty(IInstallableUnit.PROP_NAME, null);
			if (key2 == null)
				if (showingId)
					key2 = ""; //$NON-NLS-1$
				else
					key2 = iu2.getId();
		} else {
			key1 = iu1.getId();
			key2 = iu2.getId();
		}

		int result = 0;
		result = key1.compareToIgnoreCase(key2);
		if (result == 0) {
			// We want to show later versions first so compare backwards.
			result = iu2.getVersion().compareTo(iu1.getVersion());
		}
		return result;
	}
}
