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
import ro.linic.ui.pos.cloud.model.CloudReceipt;
import ro.linic.ui.pos.cloud.model.CloudReceiptLine;

@Component(property = org.osgi.framework.Constants.SERVICE_RANKING + "=1")
public class SQLiteHelperImpl implements SQLiteHelper {
	@Override
	public String receiptColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(CloudReceipt.ID_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.CLOSED_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.SYNCED_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.CREATION_TIME_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceipt.NUMBER_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	@Override
	public String receiptColumnsPlaceholder() {
		return "?,?,?,?,?,?,?";
	}
	
	@Override
	public List<Receipt> readReceipts(final ResultSet rs) throws SQLException {
		final List<Receipt> result = new ArrayList<>();
		while (rs.next()) {
			final CloudReceipt model = new CloudReceipt();
			model.setId(rs.getLong(CloudReceipt.ID_FIELD));
			final BigDecimal allowanceAmount = rs.getBigDecimal(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD);
			if (allowanceAmount != null)
				model.setAllowanceCharge(new AllowanceCharge(
						rs.getBoolean(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD), allowanceAmount));
			model.setClosed(rs.getBoolean(CloudReceipt.CLOSED_FIELD));
			model.setSynced(rs.getBoolean(CloudReceipt.SYNCED_FIELD));
			model.setCreationTime(Instant.parse(rs.getString(CloudReceipt.CREATION_TIME_FIELD)));
			model.setNumber(rs.getInt(CloudReceipt.NUMBER_FIELD));
			result.add(model);
		}
		rs.close();
		return result;
	}
	
	@Override
	public void insertReceiptInStatement(final Receipt m, final PreparedStatement stmt) throws SQLException {
		final CloudReceipt model = (CloudReceipt) m;
		stmt.setLong(1, model.getId());
    	stmt.setBoolean(2, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
    	stmt.setBigDecimal(3, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
    	stmt.setObject(4, model.getClosed(), Types.BOOLEAN);
    	stmt.setObject(5, model.getSynced(), Types.BOOLEAN);
    	stmt.setString(6, model.getCreationTime().toString());
    	stmt.setObject(7, model.getNumber(), Types.INTEGER);
	}
	
	@Override
	public String receiptLineColumns() {
		final StringBuilder sb = new StringBuilder();
		sb.append(CloudReceiptLine.ID_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.PRODUCT_ID_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.RECEIPT_ID_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.SKU_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.NAME_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.UOM_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.QUANTITY_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.PRICE_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.TAX_CODE_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.DEPARTMENT_CODE_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.CREATION_TIME_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.TAX_TOTAL_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.TOTAL_FIELD).append(LIST_SEPARATOR)
		.append(CloudReceiptLine.SYNCED_FIELD).append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.WAREHOUSE_ID_FIELD).append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.USER_ID_FIELD).append(NEWLINE);
		return sb.toString();
	}
	
	@Override
	public String receiptLineColumnsPlaceholder() {
		return "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
	}
	
	@Override
	public List<ReceiptLine> readReceiptLines(final ResultSet rs) throws SQLException {
		final List<ReceiptLine> result = new ArrayList<>();
		while (rs.next()) {
			final LegacyReceiptLine model = new LegacyReceiptLine();
			model.setId(rs.getLong(CloudReceiptLine.ID_FIELD));
			model.setProductId(rs.getLong(CloudReceiptLine.PRODUCT_ID_FIELD));
			model.setReceiptId(rs.getLong(CloudReceiptLine.RECEIPT_ID_FIELD));
			model.setSku(rs.getString(CloudReceiptLine.SKU_FIELD));
			model.setName(rs.getString(CloudReceiptLine.NAME_FIELD));
			model.setUom(rs.getString(CloudReceiptLine.UOM_FIELD));
			model.setQuantity(rs.getBigDecimal(CloudReceiptLine.QUANTITY_FIELD));
			model.setPrice(rs.getBigDecimal(CloudReceiptLine.PRICE_FIELD));
			final BigDecimal allowanceAmount = rs.getBigDecimal(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD);
			if (allowanceAmount != null)
				model.setAllowanceCharge(new AllowanceCharge(
						rs.getBoolean(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD), allowanceAmount));
			model.setTaxCode(rs.getString(CloudReceiptLine.TAX_CODE_FIELD));
			model.setDepartmentCode(rs.getString(CloudReceiptLine.DEPARTMENT_CODE_FIELD));
			model.setCreationTime(Instant.parse(rs.getString(CloudReceiptLine.CREATION_TIME_FIELD)));
			model.setTaxTotal(rs.getBigDecimal(CloudReceiptLine.TAX_TOTAL_FIELD));
			model.setTotal(rs.getBigDecimal(CloudReceiptLine.TOTAL_FIELD));
			model.setSynced(rs.getBoolean(CloudReceiptLine.SYNCED_FIELD));
			model.setWarehouseId(rs.getInt(LegacyReceiptLine.WAREHOUSE_ID_FIELD) == 0 ? null : rs.getInt(LegacyReceiptLine.WAREHOUSE_ID_FIELD));
			model.setUserId(rs.getInt(LegacyReceiptLine.USER_ID_FIELD) == 0 ? null : rs.getInt(LegacyReceiptLine.USER_ID_FIELD));
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
		stmt.setString(4, model.getSku());
		stmt.setString(5, model.getName());
		stmt.setString(6, model.getUom());
		stmt.setBigDecimal(7, model.getQuantity());
		stmt.setBigDecimal(8, model.getPrice());
    	stmt.setBoolean(9, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
    	stmt.setBigDecimal(10, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
    	stmt.setString(11, model.getTaxCode());
    	stmt.setString(12, model.getDepartmentCode());
    	stmt.setString(13, model.getCreationTime().toString());
    	stmt.setBigDecimal(14, model.getTaxTotal());
    	stmt.setBigDecimal(15, model.getTotal());
    	stmt.setObject(16, model.getSynced(), Types.BOOLEAN);
    	stmt.setObject(17, model.getWarehouseId(), Types.INTEGER);
    	stmt.setObject(18, model.getUserId(), Types.INTEGER);
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
