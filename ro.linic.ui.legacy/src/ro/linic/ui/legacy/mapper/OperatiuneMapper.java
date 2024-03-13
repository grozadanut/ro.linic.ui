package ro.linic.ui.legacy.mapper;

import ro.colibri.entities.comercial.Operatiune;
import ro.linic.ui.pos.base.model.ReceiptLine;

public class OperatiuneMapper {
	
	public static ReceiptLine toLine(final Operatiune op) {
		if (op == null)
			return null;
		
		return new ReceiptLine(op.getName(), op.getUom(), op.getCantitate(), op.getPretVanzareUnitarCuTVA(), null,
				op.getCotaTva(), op.getDepartment());
	}
}
