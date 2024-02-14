package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;

import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Element class representing a fixed set of IU's. This element should never
 * appear in a list, but can be used as a parent in a list.
 *
 * @since 3.5
 */
public class IUElementListRoot extends QueriedElement {
	Object[] children;
	private ProvisioningUI ui;

	public IUElementListRoot(final IEclipseContext ctx, final Object[] children) {
		super(ctx, null);
		this.children = children;
	}

	public IUElementListRoot(final IEclipseContext ctx) {
		this(ctx, new Object[0]);
	}

	public IUElementListRoot(final IEclipseContext ctx, final ProvisioningUI ui) {
		this(ctx, new Object[0]);
		this.ui = ui;
	}

	public void setChildren(final Object[] children) {
		this.children = children;
	}

	@Override
	protected String getImageId(final Object obj) {
		return null;
	}

	@Override
	public String getLabel(final Object o) {
		return null;
	}

	@Override
	public Object[] getChildren(final Object o) {
		return children;
	}

	@Override
	protected int getDefaultQueryType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	@Override
	public ProvisioningUI getProvisioningUI() {
		if (ui != null)
			return ui;
		return super.getProvisioningUI();
	}
}
