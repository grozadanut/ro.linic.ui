package ro.linic.ui.legacy.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.linic.ui.legacy.dialogs.PrintBarcodeDialog;
import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.service.components.BarcodePrintable;

public class PrintBarcodeHandler
{
	@Inject private Logger log;
	@Inject @OSGiBundle private Bundle bundle;
	
	@Execute
	public void execute(final EPartService partService)
	{
		final MPart activePart = partService.getActivePart();
		
		if (activePart == null || !(activePart.getObject() instanceof VanzareInterface))
			return;
		
		final ImmutableList<BarcodePrintable> printables =
				BarcodePrintable.fromProducts(((VanzareInterface) activePart.getObject()).selection());
		
		if (!printables.isEmpty())
			new PrintBarcodeDialog(Display.getCurrent().getActiveShell(), printables, log, bundle).open();
	}
}
