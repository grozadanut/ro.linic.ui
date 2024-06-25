package ro.linic.ui.legacy.service.impl;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component(property = org.osgi.framework.Constants.SERVICE_RANKING + "=1")
public class SQLiteHelperImpl implements SQLiteHelper {
	@Override
	public String receiptColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(Receipt.ID_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD).append(LIST_SEPARATOR)
		.append(Receipt.CREATION_TIME_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	@Override
	public String receiptColumnsPlaceholder() {
		return "?,?,?,?";
	}
	
	@Override
	public List<Receipt> readReceipts(final ResultSet rs) throws SQLException {
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
	
	@Override
	public void insertReceiptInStatement(final Receipt model, final PreparedStatement stmt) throws SQLException {
		stmt.setLong(1, model.getId());
    	stmt.setBoolean(2, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
    	stmt.setBigDecimal(3, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
    	stmt.setString(4, model.getCreationTime().toString());
	}
	
	@Override
	public String receiptLineColumns() {
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
		.append(ReceiptLine.CREATION_TIME_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.TAX_TOTAL_FIELD).append(LIST_SEPARATOR)
		.append(ReceiptLine.TOTAL_FIELD).append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.WAREHOUSE_ID_FIELD).append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.USER_ID_FIELD).append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.ECR_ACTIVE_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	@Override
	public String receiptLineColumnsPlaceholder() {
		return "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
	}
	
	@Override
	public List<ReceiptLine> readReceiptLines(final ResultSet rs) throws SQLException {
		final List<ReceiptLine> result = new ArrayList<>();
		while (rs.next()) {
			final LegacyReceiptLine model = new LegacyReceiptLine();
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
			model.setTaxTotal(rs.getBigDecimal(ReceiptLine.TAX_TOTAL_FIELD));
			model.setTotal(rs.getBigDecimal(ReceiptLine.TOTAL_FIELD));
			model.setWarehouseId(rs.getObject(LegacyReceiptLine.WAREHOUSE_ID_FIELD, Integer.class));
			model.setUserId(rs.getObject(LegacyReceiptLine.USER_ID_FIELD, Integer.class));
			model.setEcrActive(rs.getObject(LegacyReceiptLine.ECR_ACTIVE_FIELD, Boolean.class));
			result.add(model);
		}
		rs.close();
		return result;
	}
	
	@Override
	public void insertReceiptLineInStatement(final ReceiptLine m, final PreparedStatement stmt) throws SQLException {
		final LegacyReceiptLine model = (LegacyReceiptLine) m;
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
    	stmt.setBigDecimal(13, model.getTaxTotal());
    	stmt.setBigDecimal(14, model.getTotal());
    	stmt.setObject(15, model.getWarehouseId(), Types.INTEGER);
    	stmt.setObject(16, model.getUserId(), Types.INTEGER);
    	stmt.setObject(17, model.getEcrActive(), Types.BOOLEAN);
	}
	
	@Override
	public String productColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(Product.ID_FIELD).append(LIST_SEPARATOR)
		.append(Product.TYPE_FIELD).append(LIST_SEPARATOR)
		.append(Product.TAX_CODE_FIELD).append(LIST_SEPARATOR)
		.append(Product.DEPARTMENT_CODE_FIELD).append(LIST_SEPARATOR)
		.append(Product.SKU_FIELD).append(LIST_SEPARATOR)
		.append(Product.BARCODES_FIELD).append(LIST_SEPARATOR)
		.append(Product.NAME_FIELD).append(LIST_SEPARATOR)
		.append(Product.UOM_FIELD).append(LIST_SEPARATOR)
		.append(Product.IS_STOCKABLE_FIELD).append(LIST_SEPARATOR)
		.append(Product.PRICE_FIELD).append(LIST_SEPARATOR)
		.append(Product.STOCK_FIELD).append(LIST_SEPARATOR)
		.append(Product.IMAGE_ID_FIELD).append(LIST_SEPARATOR)
		.append(Product.TAX_PERCENTAGE_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	@Override
	public String productColumnsPlaceholder() {
		return "?,?,?,?,?,?,?,?,?,?,?,?,?";
	}
	
	@Override
	public void insertProductInStatement(final Product model, final PreparedStatement pstmt) throws SQLException, JsonProcessingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		
    	pstmt.setLong(1, model.getId());
    	pstmt.setString(2, model.getType());
    	pstmt.setString(3, model.getTaxCode());
    	pstmt.setString(4, model.getDepartmentCode());
    	pstmt.setString(5, model.getSku());
    	pstmt.setString(6, model.getBarcodes().isEmpty() ? null : objectMapper.writeValueAsString(model.getBarcodes()));
        pstmt.setString(7, model.getName());
        pstmt.setString(8, model.getUom());
        pstmt.setBoolean(9, model.isStockable());
        pstmt.setBigDecimal(10, model.getPrice());
        pstmt.setBigDecimal(11, model.getStock());
        pstmt.setString(12, model.getImageId());
        pstmt.setBigDecimal(13, model.getTaxPercentage());
	}
	
	@Override
	public List<Product> readProducts(final ResultSet rs) throws SQLException, JsonMappingException, JsonProcessingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		final List<Product> result = new ArrayList<>();
		while (rs.next()) {
			final Product p = new Product();
			p.setId(rs.getLong(Product.ID_FIELD));
			p.setType(rs.getString(Product.TYPE_FIELD));
			p.setTaxCode(rs.getString(Product.TAX_CODE_FIELD));
			p.setDepartmentCode(rs.getString(Product.DEPARTMENT_CODE_FIELD));
			p.setSku(rs.getString(Product.SKU_FIELD));
			final String dbBarcodes = rs.getString(Product.BARCODES_FIELD);
			p.setBarcodes(isEmpty(dbBarcodes) ? new HashSet<String>() : 
				objectMapper.readValue(dbBarcodes, new TypeReference<Set<String>>(){}));
			p.setName(rs.getString(Product.NAME_FIELD));
			p.setUom(rs.getString(Product.UOM_FIELD));
			p.setStockable(rs.getBoolean(Product.IS_STOCKABLE_FIELD));
			p.setPrice(rs.getBigDecimal(Product.PRICE_FIELD));
			p.setStock(rs.getBigDecimal(Product.STOCK_FIELD));
			p.setImageId(rs.getString(Product.IMAGE_ID_FIELD));
			p.setTaxPercentage(rs.getBigDecimal(Product.TAX_PERCENTAGE_FIELD));
			result.add(p);
		}
		rs.close();
		return result;
	}
}
