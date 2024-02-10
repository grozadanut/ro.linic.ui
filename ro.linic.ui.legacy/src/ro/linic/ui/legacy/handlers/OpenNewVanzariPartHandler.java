package ro.linic.ui.legacy.handlers;

import static ro.colibri.util.NumberUtils.parseToInt;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.parts.VanzareBarPart;
import ro.linic.ui.legacy.parts.VanzarePart;
import ro.linic.ui.legacy.session.ClientSession;

public class OpenNewVanzariPartHandler
{
	private HandledToolItemImpl toolItem;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.vanzari",
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService)
	{
		final int vanzarePartType = parseToInt(System.getProperty(VanzarePart.VANZARE_PART_TYPE_KEY, VanzarePart.VANZARE_PART_TYPE_DEFAULT));
		switch (vanzarePartType)
		{
		case 0:
		default:
			VanzareBarPart.newPartForBon(partService, null);
			break;
		case 1:
			VanzarePart.newPartForBon(partService, null);
			break;
		}
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.SALES_AGENT);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
