package ro.linic.ui.p2.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Adapter factory for provisioning elements
 *
 * @since 3.4
 *
 */

public class ProvUIAdapterFactory implements IAdapterFactory {
	private static final Class<?>[] CLASSES = new Class[] {IInstallableUnit.class, IProfile.class, IRepository.class, IMetadataRepository.class, IArtifactRepository.class};

	@Override
	public <T> T getAdapter(final Object adaptableObject, final Class<T> adapterType) {
		return ProvUI.getAdapter(adaptableObject, adapterType);
	}

	@Override
	public Class<?>[] getAdapterList() {
		return CLASSES;
	}

}
