package ro.linic.ui.legacy.wizards;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.linic.ui.legacy.anaf.AnafMoquiReporter;

public class EFacturaApiWizard extends EFacturaBaseWizard
{
	private IEclipseContext ctx;

	public EFacturaApiWizard(final IEclipseContext ctx, final Logger log, final AccountingDocument docIncarcat)
	{
		super(log, docIncarcat);
		this.ctx = ctx;
	}

	@Override
	public boolean performFinish()
	{
		AnafMoquiReporter.reportInvoice(ctx, docIncarcat.getCompany().getId(), docIncarcat.getId());
		return true;
	}
}
