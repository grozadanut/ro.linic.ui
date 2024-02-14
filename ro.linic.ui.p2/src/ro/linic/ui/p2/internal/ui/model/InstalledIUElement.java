package ro.linic.ui.p2.internal.ui.model;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element wrapper class for installed IU's. Used instead of the plain IU when
 * there should be a parent profile available for operations.
 *
 * @since 3.4
 */
public class InstalledIUElement extends QueriedElement implements IIUElement {

	String profileId;
	IInstallableUnit iu;
	boolean isPatch = false;

	public InstalledIUElement(final IEclipseContext ctx, final Object parent, final String profileId, final IInstallableUnit iu) {
		super(ctx, parent);
		this.profileId = profileId;
		this.iu = iu;
		this.isPatch = iu == null ? false : Boolean.valueOf(iu.getProperty(InstallableUnitDescription.PROP_TYPE_PATCH));
	}

	@Override
	protected String getImageId(final Object obj) {
		return isPatch ? ProvUIImages.IMG_PATCH_IU : ProvUIImages.IMG_IU;
	}

	@Override
	public String getLabel(final Object o) {
		return iu.getId();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IInstallableUnit.class)
			return (T) iu;
		return super.getAdapter(adapter);
	}

	public String getProfileId() {
		return profileId;
	}

	@Override
	public IInstallableUnit getIU() {
		return iu;
	}

	// TODO Later we might consider showing this in the installed views,
	// but it is less important than before install.
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
		// Should never be called, as long as shouldShowSize() returns false
	}

	@Override
	public boolean shouldShowVersion() {
		return true;
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		return iu.getRequirements();
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	@Override
	public boolean shouldShowChildren() {
		// Check that no parent has the same IU as this parent.
		// That would lead to a cycle and induce an infinite tree.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=550265
		for (Object parent = getParent(this); parent instanceof InstalledIUElement;) {
			final InstalledIUElement installedIUElement = (InstalledIUElement) parent;
			if (Objects.equals(iu, installedIUElement.getIU())) {
				return false;
			}
			parent = installedIUElement.getParent(installedIUElement);
		}
		return true;
	}

	@Override
	public Object[] getChildren(final Object o) {
		if (shouldShowChildren()) {
			// Only show children if that would not induce a cycle.
			return super.getChildren(o);
		}

		return new Object[0];
	}

	@Override
	protected Object[] getFilteredChildren(final Collection<?> results) {
		// Given the equality definition, a child cannot be equal to a sibling of this
		// because the child has a different parent.
		return results.toArray();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InstalledIUElement))
			return false;
		if (iu == null)
			return false;
		if (!iu.equals(((InstalledIUElement) obj).getIU()))
			return false;

		final Object parent = getParent(this);
		final Object objParent = ((InstalledIUElement) obj).getParent(obj);
		if (parent == this)
			return objParent == obj;
		else if (parent != null && objParent != null)
			return parent.equals(objParent);
		else if (parent == null && objParent == null)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		if (iu == null)
			return 0;
		return iu.hashCode();
	}

	@Override
	public String toString() {
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		return iu.toString();
	}

}
