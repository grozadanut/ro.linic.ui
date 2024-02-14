package ro.linic.ui.p2.internal.ui.model;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * Interface for elements that represent IU's.
 *
 * @since 3.4
 */
public interface IIUElement {

	public IInstallableUnit getIU();

	public boolean shouldShowSize();

	public boolean shouldShowVersion();

	public long getSize();

	public void computeSize(IProgressMonitor monitor);

	public Collection<IRequirement> getRequirements();

	public Object getParent(Object obj);

	public boolean shouldShowChildren();
}
