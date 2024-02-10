package ro.linic.ui.legacy.handlers;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.wizards.CustomerDebtWizard;
import ro.linic.ui.legacy.wizards.CustomerDebtWizardDialog;

public class CustomerDebtHandler
{
	private HandledToolItemImpl toolItem;
	
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.incaseaza", 
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService)
	{
		new CustomerDebtWizardDialog(Display.getCurrent().getActiveShell(), new CustomerDebtWizard(sync, bundle, log)).open();
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.INCASEAZA_DOCS);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
