package ro.linic.ui.legacy.handlers;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.parts.VanzareMoquiPart;
import ro.linic.ui.legacy.session.ClientSession;

public class OpenNewMoquiReceiptHandler {
	@Execute
	public void execute(final IEclipseContext ctx) {
		VanzareMoquiPart.newPartForBon(ctx, null);
	}

	@CanExecute
	boolean canExecute(final EPartService partService) {
		return ClientSession.instance().hasPermission(Permissions.SALES_AGENT);
	}
}
