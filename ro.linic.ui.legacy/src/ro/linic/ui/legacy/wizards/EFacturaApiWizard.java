package ro.linic.ui.legacy.wizards;

import org.eclipse.e4.core.services.log.Logger;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.linic.ui.legacy.anaf.AnafReporter;

public class EFacturaApiWizard extends EFacturaBaseWizard
{
	public EFacturaApiWizard(final Logger log, final AccountingDocument docIncarcat)
	{
		super(log, docIncarcat);
	}

	@Override
	public boolean performFinish()
	{
		AnafReporter.reportInvoice(docIncarcat.getCompany().getId(), docIncarcat.getId());
		return true;
	}
}
