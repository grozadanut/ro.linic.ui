package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Element class that represents the root of a viewer.  It can be configured
 * with its own ui and query context.
 *
 * @since 3.5
 *
 */
public abstract class RootElement extends RemoteQueriedElement {

	private IUViewQueryContext queryContext;
	private ProvisioningUI ui;

	public RootElement(final IEclipseContext ctx, final ProvisioningUI ui) {
		this(ctx, null, ProvUI.getQueryContext(ui.getPolicy()), ui);
	}

	public RootElement(final IEclipseContext ctx, final IUViewQueryContext queryContext, final ProvisioningUI ui) {
		this(ctx, null, queryContext, ui);
	}

	/*
	 * Special method for subclasses that can sometimes be a root, and sometimes not.
	 */
	protected RootElement(final IEclipseContext ctx, final Object parent, final IUViewQueryContext queryContext, final ProvisioningUI ui) {
		super(ctx, parent);
		this.queryContext = queryContext;
		this.ui = ui;
	}

	/**
	 * Set the query context that is used when querying the receiver.
	 *
	 * @param context the query context to use
	 */
	public void setQueryContext(final IUViewQueryContext context) {
		queryContext = context;
	}

	@Override
	public IUViewQueryContext getQueryContext() {
		return queryContext;
	}

	@Override
	public Policy getPolicy() {
		return ui.getPolicy();
	}

	@Override
	public ProvisioningUI getProvisioningUI() {
		return ui;
	}
}
