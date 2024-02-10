package ro.linic.ui.legacy.wizards;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.wizard.Wizard;

import ro.colibri.entities.comercial.AccountingDocument;

public abstract class EFacturaBaseWizard extends Wizard
{
	private EFacturaDetailsSupplierPage one;
	private EFacturaDetailsCustomerPage two;
	
	final protected Logger log;
	final protected AccountingDocument docIncarcat;
	
	public EFacturaBaseWizard(final Logger log, final AccountingDocument docIncarcat)
	{
		super();
		this.log = log;
		this.docIncarcat = docIncarcat;
	}

	@Override
	final public String getWindowTitle()
	{
		return "Export";
	}
	
	@Override
	final public void addPages()
	{
		one = new EFacturaDetailsSupplierPage(log);
		two = new EFacturaDetailsCustomerPage(docIncarcat.getPartner());
		addPage(one);
		addPage(two);
	}
	

	@Override
	public abstract boolean performFinish();
	
	final public void nextPressed()
	{
		two.validate();
	}
	
	final public void backPressed()
	{
		two.setPageComplete(false);
	}
	
	final protected EFacturaDetailsSupplierPage one()
	{
		return one;
	}
	
	final protected EFacturaDetailsCustomerPage two()
	{
		return two;
	}
}
