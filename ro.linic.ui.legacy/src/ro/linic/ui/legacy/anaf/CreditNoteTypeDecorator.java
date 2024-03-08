package ro.linic.ui.legacy.anaf;

import java.util.Objects;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;

public class CreditNoteTypeDecorator implements InvoiceUblType {
	private CreditNoteType creditNoteType;
	
	public CreditNoteTypeDecorator(final CreditNoteType creditNoteType) {
		super();
		this.creditNoteType = creditNoteType;
	}
	
	@Override
	public IDType getID() {
		return creditNoteType.getID();
	}

	@Override
	public SupplierPartyType getAccountingSupplierParty() {
		return creditNoteType.getAccountingSupplierParty();
	}
	
	@Override
	public MonetaryTotalType getLegalMonetaryTotal() {
		return creditNoteType.getLegalMonetaryTotal();
	}

	@Override
	public int hashCode() {
		return Objects.hash(creditNoteType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final CreditNoteTypeDecorator other = (CreditNoteTypeDecorator) obj;
		return Objects.equals(creditNoteType, other.creditNoteType);
	}

	@Override
	public String toString() {
		return "CreditNoteTypeDecorator [creditNoteType=" + creditNoteType + "]";
	}
}
