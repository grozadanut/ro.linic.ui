package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ReceiptLoader;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component
public class LocalReceiptLoader implements ReceiptLoader {
	private final static ILog log = ILog.of(LocalReceiptLoader.class);
	
	@Reference private LocalDatabase localDatabase;
	@Reference private SQLiteHelper sqliteHelper;
	
	@Override
	public List<Receipt> findAll() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<Receipt> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.receiptColumns())
		.append("FROM "+Receipt.class.getSimpleName());
		
		dbLock.readLock().lock();
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement();
				ResultSet rs = stmt.executeQuery(querySb.toString())) {
			result = sqliteHelper.readReceipts(rs);
			for (final Receipt receipt : result)
				receipt.setLines(loadLines(receipt.getId(), dbName));
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}

	@Override
	public List<Receipt> findByIdIn(final List<Long> ids) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<Receipt> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.receiptColumns())
		.append("FROM "+Receipt.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(Receipt.ID_FIELD).append(" IN ("+ids.stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR))+")");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readReceipts(rs);
			for (final Receipt receipt : result)
				receipt.setLines(loadLines(receipt.getId(), dbName));
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}

	private List<ReceiptLine> loadLines(final Long receiptId, final String dbName) throws SQLException {
		List<ReceiptLine> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.receiptLineColumns())
		.append("FROM "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(ReceiptLine.RECEIPT_ID_FIELD).append(" = ?");
		
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setLong(1, receiptId);
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readReceiptLines(rs);
		}
		
		return result;
	}
	
	@Override
	public List<Receipt> findUnclosed() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<Receipt> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.receiptColumns())
		.append("FROM "+Receipt.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(Receipt.CLOSED_FIELD).append(" IS NULL OR ").append(Receipt.CLOSED_FIELD).append(" = FALSE");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readReceipts(rs);
			for (final Receipt receipt : result)
				receipt.setLines(loadLines(receipt.getId(), dbName));
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}
}
