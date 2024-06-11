package ro.linic.ui.pos.handlers;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;
import ro.linic.ui.pos.dialogs.CreateProductDialog;

public class CreateProductHandler {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) final Shell shell, final IEclipseContext ctx) {
		final CreateProductDialog dialog = new CreateProductDialog(shell);
		ContextInjectionFactory.inject(dialog, ctx);
		dialog.open();
	}
}
