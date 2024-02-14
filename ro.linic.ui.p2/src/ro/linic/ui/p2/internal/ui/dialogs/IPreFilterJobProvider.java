package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.core.runtime.jobs.Job;

/**
 * IPreFilterJobProvider provides an optional job that must be run before
 * filtering can be allowed to occur in a filtered tree.  The client is assumed
 * to have set the expected job priority.
 *
 */
public interface IPreFilterJobProvider {
	public Job getPreFilterJob();
}
