package ro.linic.ui.p2.internal.ui.viewers;

import java.net.URI;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.IElementComparer;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.model.CategoryElement;
import ro.linic.ui.p2.internal.ui.model.IRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.ProfileElement;

public class ProvElementComparer implements IElementComparer {

	@Override
	public boolean equals(final Object a, final Object b) {
		// We treat category elements specially because this
		// is one case where resolving down to an IU will lose identity
		// differences.  (category IU's with the same name and version number cannot be treated the same).
		if (a instanceof CategoryElement || b instanceof CategoryElement)
			return a.equals(b);
		final IInstallableUnit iu1 = getIU(a);
		final IInstallableUnit iu2 = getIU(b);
		if (iu1 != null && iu2 != null)
			return iu1.equals(iu2);
		final String p1 = getProfileId(a);
		final String p2 = getProfileId(b);
		if (p1 != null && p2 != null)
			return p1.equals(p2);
		final URI r1 = getRepositoryLocation(a);
		final URI r2 = getRepositoryLocation(b);
		if (r1 != null && r2 != null)
			return r1.equals(r2);
		return a.equals(b);
	}

	@Override
	public int hashCode(final Object element) {
		if (element instanceof CategoryElement)
			return element.hashCode();
		final IInstallableUnit iu = getIU(element);
		if (iu != null)
			return iu.hashCode();
		final String profileId = getProfileId(element);
		if (profileId != null)
			return profileId.hashCode();
		final URI location = getRepositoryLocation(element);
		if (location != null)
			return location.hashCode();
		return element.hashCode();
	}

	private IInstallableUnit getIU(final Object obj) {
		return ProvUI.getAdapter(obj, IInstallableUnit.class);
	}

	private String getProfileId(final Object obj) {
		if (obj instanceof ProfileElement)
			return ((ProfileElement) obj).getLabel(obj);
		final IProfile profile = ProvUI.getAdapter(obj, IProfile.class);
		if (profile == null)
			return null;
		return profile.getProfileId();
	}

	private URI getRepositoryLocation(final Object obj) {
		if (obj instanceof IRepositoryElement<?>)
			return ((IRepositoryElement<?>) obj).getLocation();
		return null;
	}

}
