package ro.linic.ui.p2.internal.ui.model;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * Interface for elements that represent repositories.
 *
 * @since 3.4
 */
public interface IRepositoryElement<T> {

	public URI getLocation();

	public String getName();

	public String getDescription();

	public boolean isEnabled();

	public void setEnabled(boolean enabled);

	public IRepository<T> getRepository(IProgressMonitor monitor);
}
