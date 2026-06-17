package ro.linic.ui.legacy.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import jakarta.annotation.PostConstruct;
import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.parts.EFacturaPart;
import ro.linic.ui.legacy.session.ClientSession;

public class OpenEFacturaPartHandler
{
	private HandledToolItemImpl toolItem;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.efactura", 
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService)
	{
		partService.showPart(EFacturaPart.PART_ID, PartState.ACTIVATE);
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.SUPERADMIN_ROLE);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
