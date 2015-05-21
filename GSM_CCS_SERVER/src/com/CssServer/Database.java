package com.CssServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
	private Connection connection = null;
	public static final Logger logger = Logger.getLogger(Database.class.getName());
	/**
	 * Opens a specified database
	 * 
	 * <p>
	 * It will throw a ClassNotFoundException if the driver is missing.
	 * 
	 * @param database_file
	 *            Full path to the database file
	 */
	public Database(String database_file) throws ClassNotFoundException {
		// load the sqlite-JDBC driver using the current class loader
		Class.forName("org.sqlite.JDBC");
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:"
					+ database_file);
		} catch (SQLException e) {

		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				
			}
		}
	}

	/**
	 * Initializes the database logging.
	 * 
	 * <p>
	 * It will redirect the logs to the parent logger. It must be called from
	 * the parent object to set up the parameters. If not called, the logs will
	 * appear in console only (default).
	 * 
	 * @param fileHandler
	 *            Log file handler to direct the logs to.
	 * @param format
	 *            Log file format (Simple or xml)
	 */
	public void initializeLogging (Handler fileHandler, Formatter format)
	{
		logger.addHandler(fileHandler);
		logger.info("Database logging started...");
		fileHandler.setFormatter(format);
		fileHandler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
	}
}
