package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;

class SQLiteHelper {
	public static String receiptColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(Receipt.ID_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.CREATION_TIME_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	public static String receiptColumnsPlaceholder() {
		return "?,?,?,?";
	}
	
	public static List<Receipt> readReceipts(final ResultSet rs) throws SQLException {
		final List<Receipt> result = new ArrayList<>();
		while (rs.next()) {
			final Receipt model = new Receipt();
			model.setId(rs.getLong(Receipt.ID_FIELD));
			final BigDecimal allowanceAmount = rs.getBigDecimal(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD);
			if (allowanceAmount != null)
				model.setAllowanceCharge(new AllowanceCharge(
						rs.getBoolean(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD), allowanceAmount));
			model.setCreationTime(Instant.parse(rs.getString(Receipt.CREATION_TIME_FIELD)));
			result.add(model);
		}
		rs.close();
		return result;
	}
	
	public static void insertReceiptInStatement(final Receipt model, final PreparedStatement stmt) throws SQLException {
		stmt.setLong(1, model.getId());
    	stmt.setBoolean(2, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
    	stmt.setBigDecimal(3, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
    	stmt.setString(4, model.getCreationTime().toString());
	}
	
	public static String receiptLineColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(ReceiptLine.ID_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.PRODUCT_ID_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.RECEIPT_ID_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.NAME_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.UOM_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.QUANTITY_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.PRICE_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.TAX_CODE_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.DEPARTMENT_CODE_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.CREATION_TIME_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	public static String receiptLineColumnsPlaceholder() {
		return "?,?,?,?,?,?,?,?,?,?,?,?";
	}
	
	public static List<ReceiptLine> readReceiptLines(final ResultSet rs) throws SQLException {
		final List<ReceiptLine> result = new ArrayList<>();
		while (rs.next()) {
			final ReceiptLine model = new ReceiptLine();
			model.setId(rs.getLong(ReceiptLine.ID_FIELD));
			model.setProductId(rs.getLong(ReceiptLine.PRODUCT_ID_FIELD));
			model.setReceiptId(rs.getLong(ReceiptLine.RECEIPT_ID_FIELD));
			model.setName(rs.getString(ReceiptLine.NAME_FIELD));
			model.setUom(rs.getString(ReceiptLine.UOM_FIELD));
			model.setQuantity(rs.getBigDecimal(ReceiptLine.QUANTITY_FIELD));
			model.setPrice(rs.getBigDecimal(ReceiptLine.PRICE_FIELD));
			final BigDecimal allowanceAmount = rs.getBigDecimal(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD);
			if (allowanceAmount != null)
				model.setAllowanceCharge(new AllowanceCharge(
						rs.getBoolean(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD), allowanceAmount));
			model.setTaxCode(rs.getString(ReceiptLine.TAX_CODE_FIELD));
			model.setDepartmentCode(rs.getString(ReceiptLine.DEPARTMENT_CODE_FIELD));
			model.setCreationTime(Instant.parse(rs.getString(ReceiptLine.CREATION_TIME_FIELD)));
			result.add(model);
		}
		rs.close();
		return result;
	}
	
	public static void insertReceiptLineInStatement(final ReceiptLine model, final PreparedStatement stmt) throws SQLException {
		stmt.setLong(1, model.getId());
		stmt.setObject(2, model.getProductId(), Types.BIGINT);
		stmt.setObject(3, model.getReceiptId(), Types.BIGINT);
		stmt.setString(4, model.getName());
		stmt.setString(5, model.getUom());
		stmt.setBigDecimal(6, model.getQuantity());
		stmt.setBigDecimal(7, model.getPrice());
    	stmt.setBoolean(8, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
    	stmt.setBigDecimal(9, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
    	stmt.setString(10, model.getTaxCode());
    	stmt.setString(11, model.getDepartmentCode());
    	stmt.setString(12, model.getCreationTime().toString());
	}
}
