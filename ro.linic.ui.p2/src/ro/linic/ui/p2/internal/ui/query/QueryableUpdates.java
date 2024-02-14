package ro.linic.ui.p2.internal.ui.query;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An object that implements a query for available updates
 */
public class QueryableUpdates implements IQueryable<IInstallableUnit> {

	private IInstallableUnit[] iusToUpdate;
	ProvisioningUI ui;

	public QueryableUpdates(final ProvisioningUI ui, final IInstallableUnit[] iusToUpdate) {
		this.ui = ui;
		this.iusToUpdate = iusToUpdate;
	}

	@Override
	public IQueryResult<IInstallableUnit> query(final IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		final int totalWork = 2000;
		monitor.beginTask(ProvUIMessages.QueryableUpdates_UpdateListProgress, totalWork);
		final IPlanner planner = ui.getSession().getProvisioningAgent().getService(IPlanner.class);
		try {
			final Set<IInstallableUnit> allUpdates = new HashSet<>();
			for (final IInstallableUnit unit : iusToUpdate) {
				if (monitor.isCanceled())
					return Collector.emptyCollector();
				final IQueryResult<IInstallableUnit> updates = planner.updatesFor(unit,
						new ProvisioningContext(ui.getSession().getProvisioningAgent()),
						SubMonitor.convert(monitor, totalWork / 2 / iusToUpdate.length));
				allUpdates.addAll(updates.toUnmodifiableSet());
			}
			return query.perform(allUpdates.iterator());
		} catch (final OperationCanceledException e) {
			// Nothing more to do, return result
			return Collector.emptyCollector();
		} finally {
			monitor.done();
		}
	}
}
