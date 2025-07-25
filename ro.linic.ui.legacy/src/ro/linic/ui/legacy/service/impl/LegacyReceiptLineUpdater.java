package ro.linic.ui.legacy.service.impl;

import static ro.colibri.util.NumberUtils.add;
import static ro.colibri.util.NumberUtils.subtract;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataHolder;
import ro.linic.ui.pos.base.services.ReceiptLineLoader;
import ro.linic.ui.pos.base.services.ReceiptLineUpdater;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component(property = org.osgi.framework.Constants.SERVICE_RANKING + "=1")
public class LegacyReceiptLineUpdater implements ReceiptLineUpdater {
	public static void updateSyncLabel(final ReceiptLineLoader receiptLineLoader) {
		final String matchUnsyncedLines = LegacyReceiptLine.SYNCED_FIELD + " IS NULL OR "+LegacyReceiptLine.SYNCED_FIELD+" IS FALSE";
        final boolean allSynced = receiptLineLoader.findWhere(matchUnsyncedLines).isEmpty();
		ClientSession.instance().setAllSynced(allSynced);
	}
	
	@Reference private LocalDatabase localDatabase;
	@Reference private ProductDataHolder productDataHolder;
	@Reference private SQLiteHelper sqliteHelper;
	@Reference private ReceiptLineLoader receiptLineLoader;
	
	@Override
	public IStatus create(final ReceiptLine model) {
		if (model == null)
			return ValidationStatus.OK_STATUS;
		final IStatus validationStatus = validate(model);
		if (!validationStatus.isOK())
			return validationStatus;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+ReceiptLine.class.getSimpleName())
		.append("(")
		.append(sqliteHelper.receiptLineColumns())
		.append(")").append(" VALUES("+sqliteHelper.receiptLineColumnsPlaceholder()+")");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	model.setId(nextId(dbName));
        	sqliteHelper.insertReceiptLineInStatement(model, pstmt);
        	if (pstmt.executeUpdate() == 1) {
        		// update stock
        		if (model.getProductId() != null && model.getProductId() > 0)
        			decreaseStock(model.getProductId(), model.getQuantity());
        		// update sync label
        		ClientSession.instance().setAllSynced(false);
        	}
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private IStatus validate(final ReceiptLine model) {
		if (isEmpty(model.getName()))
			return ValidationStatus.error(Messages.NameMandatory);
		if (isEmpty(model.getUom()))
			return ValidationStatus.error(Messages.UOMMandatory);
		if (model.getQuantity() == null)
			return ValidationStatus.error(Messages.QuantityMandatory);
		if (model.getPrice() == null)
			return ValidationStatus.error(Messages.PriceMandatory);
		return ValidationStatus.OK_STATUS;
	}
	
	private long nextId(final String dbName) throws SQLException {
        final String sql = "SELECT MAX("+ReceiptLine.ID_FIELD+") FROM "+ReceiptLine.class.getSimpleName() +
        		" WHERE "+ReceiptLine.ID_FIELD+" < 100000";
        long nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        }
        
        return nextId;
    }
	
	@Override
	public IStatus update(final long id, final ReceiptLine m) {
		if (m == null || m.getId() == null)
			return ValidationStatus.OK_STATUS;
		
		final LegacyReceiptLine model = (LegacyReceiptLine) m;
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+ReceiptLine.class.getSimpleName()+" SET ")
		.append(LegacyReceiptLine.ID_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.RECEIPT_ID_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(LegacyReceiptLine.SYNCED_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(LegacyReceiptLine.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setLong(1, model.getId());
            pstmt.setLong(2, model.getReceiptId());
            pstmt.setBoolean(3, model.synced());
            // WHERE
            pstmt.setLong(4, id);
            pstmt.executeUpdate();
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	@Override
	public IStatus updateQuantity(final long id, final BigDecimal newQuantity) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+ReceiptLine.class.getSimpleName()+" SET ")
		.append(ReceiptLine.QUANTITY_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	final LegacyReceiptLine receiptLine = (LegacyReceiptLine) findReceiptLinesByIdIn(Set.of(id))
        			.stream().findFirst().get();
        	
        	if (receiptLine.synced())
    			return ValidationStatus.error(ro.linic.ui.legacy.session.Messages.LegacyReceiptLineUpdater_UpdateQuantitySyncErr);
        	
        	final BigDecimal oldQuantity = receiptLine.getQuantity();
        	
            pstmt.setBigDecimal(1, newQuantity);
            // WHERE
            pstmt.setLong(2, id);
            if (pstmt.executeUpdate() == 1) {
            	// update product stock
            	final BigDecimal quantityAdded = subtract(newQuantity, oldQuantity);
            	if (receiptLine.getProductId() != null && receiptLine.getProductId() > 0)
        			decreaseStock(receiptLine.getProductId(), quantityAdded);
            }
            
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	@Override
	public IStatus delete(final Set<Long> ids) {
		if (ids == null || ids.isEmpty())
			return ValidationStatus.OK_STATUS;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final String sql = "DELETE FROM "+ReceiptLine.class.getSimpleName()+" WHERE "+ReceiptLine.ID_FIELD+" == ?";

		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sql)) {
        	final List<ReceiptLine> linesToDelete = findReceiptLinesByIdIn(ids);
        	final Set<Long> receiptIds = linesToDelete.stream()
        			.map(ReceiptLine::getReceiptId)
        			.collect(Collectors.toSet());
        	
        	// 1. delete lines
        	for (final ReceiptLine l : linesToDelete) {
        		final LegacyReceiptLine lineToDelete = (LegacyReceiptLine) l;
        		pstmt.setLong(1, lineToDelete.getId());
        		if (pstmt.executeUpdate() == 1) {
        			// update product stock if line is unsynced
        			if (lineToDelete.getProductId() != null && lineToDelete.getProductId() > 0 && !lineToDelete.synced())
        				increaseStock(lineToDelete.getProductId(), lineToDelete.getQuantity());
        		}
			}
        	
        	// 2. delete receipt if empty
        	for (final Long receiptId : receiptIds)
				if (hasNoLine(receiptId))
					deleteReceipt(receiptId);
        	
            updateSyncLabel(receiptLineLoader);
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private List<ReceiptLine> findReceiptLinesByIdIn(final Set<Long> ids) throws SQLException {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		List<ReceiptLine> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.receiptLineColumns())
		.append("FROM "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD).append(" IN ("+ids.stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR))+")");
		
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readReceiptLines(rs);
		}
		
		return result;
	}
	
	private boolean hasNoLine(final Long receiptId) throws SQLException {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD).append(NEWLINE)
		.append("FROM "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(ReceiptLine.RECEIPT_ID_FIELD).append(" = ?");
		
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setLong(1, receiptId);
			final ResultSet rs = stmt.executeQuery();
			return !rs.next();
		}
	}
	
	private void deleteReceipt(final Long receiptId) throws SQLException {
		if (receiptId == null)
			return;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		final String sql = "DELETE FROM "+Receipt.class.getSimpleName()+" WHERE "+Receipt.ID_FIELD+" == ?";

        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sql)) {
        	pstmt.setLong(1, receiptId);
        	pstmt.executeUpdate();
        }
	}
	
	private void increaseStock(final Long productId, final BigDecimal quantityToAdd) throws SQLException {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Product.class.getSimpleName()+" SET ")
		.append(Product.STOCK_FIELD+" = "+Product.STOCK_FIELD+" + ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Product.ID_FIELD+" = ?");
		
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
            pstmt.setBigDecimal(1, quantityToAdd);
            // WHERE
            pstmt.setLong(2, productId);
            if (pstmt.executeUpdate() == 1)
            	productDataHolder.getData().stream()
            	.filter(p -> p.getId() == productId)
            	.findFirst()
            	.ifPresent(p -> p.setStock(add(p.getStock(), quantityToAdd)));
        }
	}
	
	private void decreaseStock(final Long productId, final BigDecimal quantityToSubtract) throws SQLException {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Product.class.getSimpleName()+" SET ")
		.append(Product.STOCK_FIELD+" = "+Product.STOCK_FIELD+" - ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Product.ID_FIELD+" = ?");
		
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
            pstmt.setBigDecimal(1, quantityToSubtract);
            // WHERE
            pstmt.setLong(2, productId);
            if (pstmt.executeUpdate() == 1)
            	productDataHolder.getData().stream()
            	.filter(p -> p.getId() == productId)
            	.findFirst()
            	.ifPresent(p -> p.setStock(subtract(p.getStock(), quantityToSubtract)));
        }
	}
}
