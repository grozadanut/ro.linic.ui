package ro.linic.ui.legacy.wizards;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Bundle;

import ro.colibri.entities.comercial.AccountingDocument;

public class InchideBonWizard extends Wizard
{
	public static final int BUTTON_WIDTH = 110;
	public static final int BUTTON_HEIGHT = 200;
	public static final int READ_ONLY_TEXT_WIDTH = 120;
	public static final int EDITABLE_TEXT_WIDTH = 100;
	
	public static final int DROPDOWN_WIDTH = 200;
	public static final int DELEGAT_CONTAINER_WIDTH = 400;
	public static final int DELEGAT_CONTAINER_HEIGHT = 200;
	
	public static enum TipInchidere
	{
		PRIN_CASA, FACTURA_BC, PRIN_CARD, ANY;
	}
	
	private InchideBonFirstPage one;
	private InchideBonFacturaOrBCPage two;
	
	private TipInchidere tipInchidere;
	private AccountingDocument bonCasa;
	private boolean casaActive;
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	
	public InchideBonWizard(final AccountingDocument bonCasa, final boolean casaActive,
			final UISynchronize sync, final Bundle bundle, final Logger log, final TipInchidere tipInchidere)
	{
		super();
		this.bonCasa = bonCasa;
		this.casaActive = casaActive;
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
		this.tipInchidere = tipInchidere;
	}

	@Override
	public String getWindowTitle()
	{
		return "Inchide Bon";
	}

	@Override
	public void addPages()
	{
		one = new InchideBonFirstPage(bonCasa, casaActive, bundle, log, tipInchidere);
		two = new InchideBonFacturaOrBCPage(bonCasa, casaActive, sync, bundle, log);
		if (!TipInchidere.FACTURA_BC.equals(tipInchidere))
			addPage(one);
		if (!TipInchidere.PRIN_CARD.equals(tipInchidere) && !TipInchidere.PRIN_CASA.equals(tipInchidere))
			addPage(two);
	}

	@Override
	public boolean performFinish()
	{
		return true;
	}
	
	public InchideBonFirstPage getPageOne()
	{
		return one;
	}
	
	public InchideBonFacturaOrBCPage getPageTwo()
	{
		return two;
	}
}
