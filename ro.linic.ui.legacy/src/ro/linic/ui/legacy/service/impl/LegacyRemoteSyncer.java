package ro.linic.ui.legacy.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ListUtils;
import ro.colibri.util.NumberUtils;
import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.pos.cloud.model.CloudReceipt;
import ro.linic.ui.pos.cloud.model.CloudReceiptLine;
import ro.linic.ui.pos.cloud.services.RemoteSyncer;

@Component
public class LegacyRemoteSyncer implements RemoteSyncer {
	private static Stream<Operatiune> toOp(final CloudReceipt receipt) {
		return receipt.getLines().stream()
				.map(LegacyReceiptLine.class::cast)
				.filter(crl -> !crl.synced())
				.map(line -> {
					final Operatiune op = new Operatiune();
					final AccountingDocument accDoc = new AccountingDocument();
					accDoc.setNrDoc(String.valueOf(receipt.getNumber()));
					accDoc.setDoc(AccountingDocument.BON_CASA_NAME);
					accDoc.setDataDoc(LocalDateTime.ofInstant(receipt.getCreationTime(), ZoneId.systemDefault()));
					
					op.setAccDoc(accDoc);
					op.setBarcode(line.getSku());
					op.setCantitate(line.getQuantity());
					op.setCotaTva(line.getTaxCode());
					op.setDataOp(LocalDateTime.ofInstant(line.getCreationTime(), ZoneId.systemDefault()));
					op.setDepartment(line.getDepartmentCode());
					op.setGestiune(new Gestiune().setId(line.getWarehouseId()));
					op.setName(line.getName());
					op.setOperator(new User().setId(line.getUserId()));
					op.setPretVanzareUnitarCuTVA(line.getPrice());
					op.setTipOp(TipOp.IESIRE);
					op.setUom(line.getUom());
					op.setValoareVanzareFaraTVA(NumberUtils.subtract(line.getTotal(), line.getTaxTotal()));
					op.setValoareVanzareTVA(line.getTaxTotal());
					return op;
				});
	}
	
	@Override
	public IStatus syncReceiptLines(final Collection<CloudReceiptLine> lines, final Collection out) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public IStatus syncReceipts(final Collection<CloudReceipt> receipts, final Collection out) {
		final ImmutableList<Operatiune> ops = receipts.stream()
				.flatMap(LegacyRemoteSyncer::toOp)
				.collect(ListUtils.toImmutableList());
		
		if (ops.isEmpty())
			return ValidationStatus.OK_STATUS;
		
		// remote does not return the saved entity, so no way to get the remote ID here
		final InvocationResult result = BusinessDelegate.saveLocalOps(ops);
		if (result.statusCanceled())
			return ValidationStatus.error(result.toTextDescription());
		return ValidationStatus.OK_STATUS;
	}
}
