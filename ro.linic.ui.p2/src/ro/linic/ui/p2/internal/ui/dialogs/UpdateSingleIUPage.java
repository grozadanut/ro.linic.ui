package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.internal.ui.viewers.IUDetailsLabelProvider;
import ro.linic.ui.p2.ui.ProvisioningUI;

public class UpdateSingleIUPage extends ProvisioningWizardPage {

	UpdateOperation operation;
	private IEclipseContext ctx;

	protected UpdateSingleIUPage(final IEclipseContext ctx, final UpdateOperation operation, final ProvisioningUI ui) {
		super("UpdateSingleIUPage", ui, null); //$NON-NLS-1$
		this.ctx = ctx;
		setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		final IProduct product = Platform.getProduct();
		final String productName = product != null && product.getName() != null ? product.getName() : ProvUIMessages.ApplicationInRestartDialog;
		setDescription(NLS.bind(ProvUIMessages.UpdateSingleIUPage_SingleUpdateDescription, productName));
		Assert.isNotNull(operation);
		Assert.isTrue(operation.hasResolved());
		Assert.isTrue(operation.getSelectedUpdates().length == 1);
		Assert.isTrue(operation.getResolutionResult().isOK());
		this.operation = operation;
	}

	@Override
	public void createControl(final Composite parent) {
		final IInstallableUnit updateIU = getUpdate().replacement;
		String url = null;
		if (updateIU.getUpdateDescriptor().getLocation() != null)
			try {
				url = URIUtil.toURL(updateIU.getUpdateDescriptor().getLocation()).toExternalForm();
			} catch (final MalformedURLException e) {
				// ignore and null URL will be ignored below
			}
		if (url != null) {
			Browser browser = null;
			try {
				browser = new Browser(parent, SWT.NONE);
				browser.setUrl(url);
				browser.setBackground(parent.getBackground());
				final Point size = getProvisioningUI().getPolicy().getUpdateDetailsPreferredSize();
				if (size != null) {
					browser.setSize(size);
				}
				setControl(browser);
				return;
			} catch (final SWTError e) {
				// Fall through to backup plan.
			}
		}
		// Create a text description of the update.
		final Text text = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);
		text.setBackground(parent.getBackground());
		text.setText(getUpdateText(updateIU));
		setControl(text);
	}

	private String getUpdateText(final IInstallableUnit iu) {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(new IUDetailsLabelProvider(ctx).getClipboardText(getUpdate().replacement, CopyUtils.DELIMITER));
		buffer.append(CopyUtils.NEWLINE);
		buffer.append(CopyUtils.NEWLINE);
		String text = iu.getUpdateDescriptor().getDescription();
		if (text != null)
			buffer.append(text);
		else {
			text = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION);
			if (text != null)
				buffer.append(text);
		}
		return buffer.toString();

	}

	public boolean performFinish() {
		if (operation.getResolutionResult().getSeverity() != IStatus.ERROR) {
			getProvisioningUI().schedule(operation.getProvisioningJob(null), StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	@Override
	protected String getClipboardText(final Control control) {
		return getUpdate().toString();
	}

	private Update getUpdate() {
		return operation.getSelectedUpdates()[0];
	}

}
