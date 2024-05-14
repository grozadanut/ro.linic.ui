package ro.linic.ui.legacy.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;

import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.dialogs.PuncteFidelitateDialog;
import ro.linic.ui.legacy.session.ClientSession;

public class PuncteFidelitateHandler
{
	private HandledToolItemImpl toolItem;
	
	@Inject private UISynchronize sync;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.puncte_fidelitate", 
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService)
	{
		new PuncteFidelitateDialog(Display.getCurrent().getActiveShell(), sync).open();
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.SALES_AGENT);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
