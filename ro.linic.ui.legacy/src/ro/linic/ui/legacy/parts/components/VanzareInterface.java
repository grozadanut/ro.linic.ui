package ro.linic.ui.legacy.parts.components;

import java.util.Collection;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Product;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;

public interface VanzareInterface
{
	void updateBonCasa(AccountingDocument bonCasa, boolean updateTotalLabels);
	Logger log();
	AccountingDocument getBonCasa();
	Bundle getBundle();
	Collection<Product> selection();
	void closeBon(final TipInchidere tipInchidere);
	boolean canCloseReceipt();
}
