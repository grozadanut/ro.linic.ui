package ro.linic.ui.legacy.service.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.legacy.session.ClientSession;

@Component
public class SQLiteDatabase implements LocalDatabase {
	private final static Logger log = Logger.getLogger(SQLiteDatabase.class.getName());
	
	private Map<String, Connection> connections = new HashMap<>();
	private Map<String, ReadWriteLock> locks = new HashMap<>();
	
	@Deactivate
	public void deactivate() {
		closeConnections();
		locks.clear();
	}
	
	@Override
	public Connection getConnection(final String dbName) {
		final String tenantDbName = ClientSession.instance().getLoggedUser().getSelectedGestiune().getId() + "-" + dbName;
		Connection conn = connections.get(tenantDbName);
		if (conn == null) {
			try {
				Class.forName("org.sqlite.JDBC");
				conn = DriverManager.getConnection("jdbc:sqlite:"+tenantDbName);
				connections.put(tenantDbName, conn);
			} catch (final SQLException | ClassNotFoundException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return conn;
	}
	
	@Override
	public synchronized ReadWriteLock getLock(final String dbName) {
		ReadWriteLock lock = locks.get(dbName);
		if (lock == null) {
			lock = new ReentrantReadWriteLock();
			locks.put(dbName, lock);
		}
		return lock;
	}

	@Override
	public void closeConnections(final String... dbNames) {
		try {
			if (dbNames == null || dbNames.length < 1) {
				for (final Connection conn : connections.values())
					conn.close();
				connections.clear();
			} else {
				for (final String dbName : dbNames) {
					final String tenantDbName = ClientSession.instance().getLoggedUser().getSelectedGestiune().getId() + "-" + dbName;
					final Connection conn = connections.get(tenantDbName);
					if (conn != null) {
						conn.close();
						connections.remove(tenantDbName);
					}
				}
			}
		} catch (final SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
