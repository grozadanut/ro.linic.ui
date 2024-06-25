package ro.linic.ui.pos.base.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;

public interface SQLiteHelper {
	public String receiptColumns();
	public String receiptColumnsPlaceholder();
	public List<Receipt> readReceipts(final ResultSet rs) throws SQLException;
	public void insertReceiptInStatement(final Receipt model, final PreparedStatement stmt) throws SQLException;
	public String receiptLineColumns();
	public String receiptLineColumnsPlaceholder();
	public List<ReceiptLine> readReceiptLines(final ResultSet rs) throws SQLException;
	public void insertReceiptLineInStatement(final ReceiptLine model, final PreparedStatement stmt) throws SQLException;
	public String productColumns();
	public String productColumnsPlaceholder();
	public void insertProductInStatement(final Product model, final PreparedStatement pstmt) throws SQLException, JsonProcessingException;
	public List<Product> readProducts(final ResultSet rs) throws SQLException, JsonMappingException, JsonProcessingException;
}
