package ro.linic.ui.p2.internal.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element wrapper class for IU's that represent categories of available IU's
 *
 * @since 3.4
 */
public class CategoryElement extends RemoteQueriedElement implements IIUElement {

	private ArrayList<IInstallableUnit> ius = new ArrayList<>(1);
	private Collection<IRequirement> requirements;
	private Object[] cache = null;

	public CategoryElement(final IEclipseContext ctx, final Object parent, final IInstallableUnit iu) {
		super(ctx, parent);
		ius.add(iu);
	}

	@Override
	protected String getImageId(final Object obj) {
		return ProvUIImages.IMG_CATEGORY;
	}

	@Override
	public String getLabel(final Object o) {
		final IInstallableUnit iu = getIU();
		if (iu != null)
			return iu.getId();
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IInstallableUnit.class)
			return (T) getIU();
		return super.getAdapter(adapter);
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	@Override
	public IInstallableUnit getIU() {
		if (ius == null || ius.isEmpty())
			return null;
		return ius.get(0);
	}

	@Override
	public long getSize() {
		return ProvUI.SIZE_UNKNOWN;
	}

	@Override
	public boolean shouldShowSize() {
		return false;
	}

	@Override
	public void computeSize(final IProgressMonitor monitor) {
		// Should never be called, since shouldShowSize() returns false
	}

	@Override
	public boolean shouldShowVersion() {
		return false;
	}

	public void mergeIU(final IInstallableUnit iu) {
		ius.add(iu);
	}

	public boolean shouldMerge(final IInstallableUnit iu) {
		final IInstallableUnit myIU = getIU();
		if (myIU == null)
			return false;
		return getMergeKey(myIU).equals(getMergeKey(iu));
	}

	private String getMergeKey(final IInstallableUnit iu) {
		String mergeKey = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (mergeKey == null || mergeKey.length() == 0) {
			mergeKey = iu.getId();
		}
		return mergeKey;
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		if (ius == null || ius.isEmpty())
			return Collections.emptyList();
		if (requirements == null) {
			if (ius.size() == 1)
				requirements = getIU().getRequirements();
			else {
				final ArrayList<IRequirement> capabilities = new ArrayList<>();
				for (final IInstallableUnit iu : ius) {
					capabilities.addAll(iu.getRequirements());
				}
				requirements = capabilities;
			}
		}
		return requirements;
	}

	@Override
	protected Object[] fetchChildren(final Object o, final IProgressMonitor monitor) {
		if (cache == null)
			cache = super.fetchChildren(o, monitor);
		return cache;
	}

	@Override
	public boolean shouldShowChildren() {
		return true;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CategoryElement))
			return false;
		final IInstallableUnit myIU = getIU();
		final IInstallableUnit objIU = ((CategoryElement) obj).getIU();
		if (myIU == null || objIU == null)
			return false;
		return getMergeKey(myIU).equals(getMergeKey(objIU));
	}

	@Override
	public int hashCode() {
		final IInstallableUnit iu = getIU();
		final int prime = 23;
		int result = 1;
		result = prime * result + ((iu == null) ? 0 : getMergeKey(iu).hashCode());
		return result;
	}

	@Override
	public String toString() {
		final IInstallableUnit iu = getIU();
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		final StringBuilder result = new StringBuilder();
		result.append("Category Element - "); //$NON-NLS-1$
		result.append(getMergeKey(iu));
		result.append(" (merging IUs: "); //$NON-NLS-1$
		result.append(ius.toString());
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}
}
