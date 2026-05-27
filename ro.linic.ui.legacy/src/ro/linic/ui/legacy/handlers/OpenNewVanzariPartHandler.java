package ro.linic.ui.legacy.handlers;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;
import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.parts.VanzareBarPart;
import ro.linic.ui.legacy.parts.VanzareMoquiPart;
import ro.linic.ui.legacy.parts.VanzarePart;
import ro.linic.ui.legacy.preferences.PreferenceKey;
import ro.linic.ui.legacy.preferences.PreferenceKey.SalesPartType;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.security.services.AuthenticationSession;

public class OpenNewVanzariPartHandler {
	public static void openNewSalesPart(final IEclipseContext ctx) {
		final Bundle bundle = FrameworkUtil.getBundle(PreferenceKey.class);
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		
		final String vanzarePartType = prefs.get(PreferenceKey.VANZARE_PART_TYPE_KEY, 
				System.getProperty(VanzarePart.VANZARE_PART_TYPE_KEY, PreferenceKey.VANZARE_PART_TYPE_DEFAULT.name()));
		
		if ("1".equals(vanzarePartType) || SalesPartType.STANDARD.name().equals(vanzarePartType)) {
		    VanzarePart.newPartForBon(ctx, null);
		} else if (SalesPartType.BETA.name().equals(vanzarePartType)) {
		    VanzareMoquiPart.newPartForBon(ctx, null);
		} else {
		    // This catches "0", SalesPartType.CAFE, and any other default values
		    VanzareBarPart.newPartForBon(ctx, null);
		}
	}

	private HandledToolItemImpl toolItem;

	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService) {
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.vanzari",
				application.getToolBarContributions().iterator().next());
	}

	@Execute
	public void execute(final IEclipseContext ctx) {
		openNewSalesPart(ctx);
	}

	@CanExecute
	boolean canExecute(final EPartService partService, final AuthenticationSession auth) {
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.SALES_AGENT);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
