package ro.linic.ui.legacy.handlers;

import java.util.List;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import jakarta.inject.Named;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.legacy.dialogs.OrderProductsDialog;
import ro.linic.ui.security.services.AuthenticationSession;

public class OrderProductsHandler {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> requirements, final DataServices dataServices,
			@Named(IServiceConstants.ACTIVE_SHELL) final Shell shell, final UISynchronize sync, final AuthenticationSession authSession,
			@OSGiBundle final Bundle bundle, final Logger log) {
		new OrderProductsDialog(shell, authSession, sync, dataServices, bundle, log, requirements).open();
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> requirements) {
		return requirements != null && !requirements.isEmpty();
	}
}
