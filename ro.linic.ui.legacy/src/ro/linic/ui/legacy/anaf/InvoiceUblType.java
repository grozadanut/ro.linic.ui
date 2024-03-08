package ro.linic.ui.legacy.anaf;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType;

public interface InvoiceUblType {
	IDType getID();
	SupplierPartyType getAccountingSupplierParty();
	MonetaryTotalType getLegalMonetaryTotal();
}
