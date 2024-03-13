package ro.linic.ui.legacy.mapper;

import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.greaterThan;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.util.NumberUtils;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;

public class AccDocMapper {
	
	public static Receipt toReceipt(final Collection<AccountingDocument> accDocs) {
		final AccountingDocument accDoc = comasareBonuri(accDocs);
		
		final List<ReceiptLine> lines = accDoc.getOperatiuni().stream()
				.filter(op -> greaterThan(op.getCantitate(), BigDecimal.ZERO))
				.map(OperatiuneMapper::toLine)
				.collect(Collectors.toList());
		
		// subtotal(discount applies to accDoc subtotal)
		// discount
		final Optional<AllowanceCharge> allowanceCharge = accDoc.getOperatiuni().stream()
				.filter(op -> NumberUtils.smallerThan(op.getCantitate(), BigDecimal.ZERO))
				.map(op -> op.getCantitate().multiply(op.getPretVanzareUnitarCuTVA()).abs())
				.reduce(BigDecimal::add)
				.map(total -> new AllowanceCharge(false, total));
		
		return new Receipt(lines, allowanceCharge.orElse(null));
	}
	
	private static AccountingDocument comasareBonuri(final Collection<AccountingDocument> docs)
	{
		final AccountingDocument comasat = new AccountingDocument();
		comasat.setOperatiuni(docs.stream()
				.flatMap(AccountingDocument::getOperatiuni_Stream)
				.collect(toImmutableSet()));
		return comasat;
	}
}
