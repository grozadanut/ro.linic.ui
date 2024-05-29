package ro.linic.ui.base.services;

import java.sql.Connection;
import java.util.concurrent.locks.ReadWriteLock;

public interface LocalDatabase {
	Connection getConnection(String dbName);
	ReadWriteLock getLock(String dbName);
	/**
	 * @param dbName to close or null to close all connections
	 */
	void closeConnections(String... dbNames);
}
