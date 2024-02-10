package ro.linic.ui.legacy.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import ro.colibri.entities.comercial.Operatiune.Recenzie;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class RecenzieNeutruHandler
{
	@Execute
	public void execute(final Shell shell)
	{
		if (BusinessDelegate.persistRecenzie(Recenzie.NEUTRU).statusOk())
			MessageDialog.openInformation(shell, "Recenzie", "Ai primi un LIKE! Nu te lasa, poti si mai mult de atat!");
	}
}
