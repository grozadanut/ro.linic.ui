package ro.linic.ui.legacy.anaf;

import java.util.Objects;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

public class InvoiceTypeDecorator implements InvoiceUblType {
	private InvoiceType invoiceType;
	
	public InvoiceTypeDecorator(final InvoiceType invoiceType) {
		super();
		this.invoiceType = invoiceType;
	}
	
	@Override
	public IDType getID() {
		return invoiceType.getID();
	}

	@Override
	public SupplierPartyType getAccountingSupplierParty() {
		return invoiceType.getAccountingSupplierParty();
	}
	
	@Override
	public MonetaryTotalType getLegalMonetaryTotal() {
		return invoiceType.getLegalMonetaryTotal();
	}

	@Override
	public int hashCode() {
		return Objects.hash(invoiceType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final InvoiceTypeDecorator other = (InvoiceTypeDecorator) obj;
		return Objects.equals(invoiceType, other.invoiceType);
	}

	@Override
	public String toString() {
		return "InvoiceTypeDecorator [invoiceType=" + invoiceType + "]";
	}
}
