package ro.linic.ui.legacy.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import ro.colibri.security.Permissions;
import ro.colibri.util.StringUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.dialogs.RegisterZDialog;
import ro.linic.ui.legacy.dialogs.RegisterZDialogCafe;
import ro.linic.ui.legacy.preferences.PreferenceKey;
import ro.linic.ui.legacy.session.ClientSession;

public class RegisterZHandler
{
	private HandledToolItemImpl toolItem;
	
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.registerZ", 
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService,
			@Optional @Preference(nodePath = PreferenceKey.NODE_PATH, value = PreferenceKey.REGISTER_Z_DIALOG_KEY) final String dialogType)
	{
		if (StringUtils.globalIsMatch(dialogType, PreferenceKey.REGISTER_Z_DIALOG_CAFE_VALUE, TextFilterMethod.EQUALS))
			new RegisterZDialogCafe(Display.getCurrent().getActiveShell(), sync, bundle, log).open();
		else
			new RegisterZDialog(Display.getCurrent().getActiveShell(), sync, bundle, log).open();
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.REGISTER_Z);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
