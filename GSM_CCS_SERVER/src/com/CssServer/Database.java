package com.CssServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.CssServer.GroupNameEntry;
import com.CssServer.MembershipEntry;

import javax.swing.text.TabExpander;

public class Database {
	private static final String GROUP_NAME_TABLE = "GroupName";
	private static final String MEMBERSHIP_TABLE = "Membership";
	private Connection connection = null;
	private Statement statement = null;
	public static final Logger logger = Logger.getLogger(Database.class
			.getName());

	/**
	 * Opens a specified database
	 * 
	 * <p>
	 * It will throw a ClassNotFoundException if the driver is missing.
	 * 
	 * @param database_file
	 *            Full path to the database file
	 * @throws ClassNotFoundException
	 *            If the database file not found or is corrupted.
	 */
	public Database(String database_file) throws ClassNotFoundException {
		// load the sqlite-JDBC driver using the current class loader
		Class.forName("org.sqlite.JDBC");
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:"
					+ database_file);
			statement = connection.createStatement();
			statement.setQueryTimeout(30);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
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
	public void initializeLogging(Handler fileHandler, Formatter format) {
		logger.addHandler(fileHandler);
		logger.info("Database logging started...");
		fileHandler.setFormatter(format);
		fileHandler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
	}
	
	/**
	 * Creates the groupName table in the database.
	 * 
	 * <p>
	 * The table has three parameters:
	 * 1) The group name
	 * 2) Latitude of the location
	 * 3) Longitude of the location
	 * 
	 */
	public void createGroupNameTable()
	{
		createTable(GROUP_NAME_TABLE, "(name string, latitude double, longitude double)");
		
		return;
	}
	
	/**
	 * Creates the membership table in the database.
	 * 
	 * <p>
	 * The table has four parameters:
	 * 1) The user id
	 * 2) The group the user belongs to
	 * 3) The latitude of the group location
	 * 4) The longitude of the group location
	 */
	public void createMembershipTable()
	{
		createTable(MEMBERSHIP_TABLE, "(userid string, membership string, latitude double, longitude double)");
		
		return;
	}
	
	/**
	 * Returns all the entries of the membership table
	 * 
	 * <p>
	 * Returns a list of the table entries. The table specifies the
	 * the current user ids and the groups they belong to.
	 * 
	 * @return
	 * 		returns a vector containing the MembershipEntry objects.
	 */
	public Vector<MembershipEntry> getMembershipTableEntries() {
		Vector<MembershipEntry> tableEntries = new Vector<MembershipEntry>();
		String query = "select * from " + MEMBERSHIP_TABLE;
		try {
			ResultSet rs = statement.executeQuery(query);
			while(rs.next())
			{
				MembershipEntry entry = new MembershipEntry();
				entry.userid = rs.getString("userid");
				entry.membership = rs.getString("membership");
				entry.latitude = rs.getDouble("latitude");
				entry.longitude = rs.getDouble("longitude");
				tableEntries.addElement(entry);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.severe("Database problems while executing query in executeQuery. Query: "
					+ query);
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return tableEntries;
	}
	
	/**
	 * Returns all the entries of the GroupName table
	 * 
	 * <p>
	 * Returns a list of the table entries. The table specifies the
	 * group names and the associated latitude and longitudes.
	 * 
	 * @return
	 * 		returns a vector containing the GourpNameEntry objects.
	 */
	public Vector<GroupNameEntry> getGroupNameTableEntries() {
		Vector<GroupNameEntry> tableEntries = new Vector<GroupNameEntry>();
		String query = "select * from " + GROUP_NAME_TABLE;
		try {
			ResultSet rs = statement.executeQuery(query);
			while(rs.next())
			{
				GroupNameEntry entry = new GroupNameEntry();
				entry.name = rs.getString("name");
				entry.latitude = rs.getDouble("latitude");
				entry.longitude = rs.getDouble("longitude");
				tableEntries.addElement(entry);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.severe("Database problems while executing query in executeQuery. Query: "
					+ query);
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return tableEntries;
	}
	
	/**
	 * Adds the specified entry to the GroupName table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param _groupName
	 *            The name of the group
	 * @param _latitude
	 *            The latitude of the group location
	 * @param _longitude
	 * 			  The longitude of the group location
	 * @return
	 *        True is succeeds. False otherwise.
	 */
	public boolean addEntryToGroupNameTable(String _groupName,
			double _latitude, double _longitude) {
		String entry = "('" + _groupName + "', " + Double.toString(_latitude)
				+ ", " + Double.toString(_longitude) + ")";
		
		return addEntryToTable(entry, GROUP_NAME_TABLE);
	}
	
	/**
	 * Adds the specified entry to the Membership table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param _userid
	 *            The id of the user
	 * @param _groupMembership
	 *            The group that the user belongs to
	 * @param _latitude
	 *            The latitude of the group location
	 * @param _longitude
	 * 			  The longitude of the group location
	 * @return
	 *        True if succeeds. False otherwise.
	 */
	public boolean addEntryToMembershipTable(String _userid, String _groupMembership,
			double _latitude, double _longitude) {
		String entry = "('" + _userid + "', " +
			           "'"+ _groupMembership + "', " + 
				       Double.toString(_latitude) + ", " + 
			           Double.toString(_longitude) + ")";
		
		return addEntryToTable(entry, MEMBERSHIP_TABLE);
	}
	
	/**
	 * Removes the specified entry from the GroupName table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param _groupName
	 *            The group name to remove.
	 * @param _latitude 
	 *            The latitude of the group location.
	 * @param _longitude
	 *            The longitude of the group location.
	 * 
	 * @return
	 *        True if succeeds. False otherwise.
	 */
	public boolean removeEntryFromGroupNameTable(String _groupName,
			double _latitude, double _longitude) {
		String entry = "name = " + _groupName + " AND latitude = " + _latitude + " AND longitude = " + _longitude;	
		
		return removeEntryFromTable(entry, GROUP_NAME_TABLE);
	}
	
	/**
	 * Removes the specified entry from the Membership table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param _userid
	 *            The entry to be removed.
	 * @return
	 *        True if succeeds. False otherwise.
	 */
	public boolean removeEntryFromMembershipTable(String _userid) {
		String entry = "userid = " + _userid;
		
		return removeEntryFromTable(entry, MEMBERSHIP_TABLE);
	}
	
	/**
	 * Removes the GroupName table from the database.
	 * 
	 * <p>
	 * Should never be called. This helper method is provided
	 * for testing purposes.
	 * 
	 */
	public void removeGroupNameTable()
	{
		removeTable(GROUP_NAME_TABLE);
		return;
	}
	
	/**
	 * Removes the Membership table from the database.
	 * 
	 * <p>
	 * Should never be called. This helper method is provided
	 * for testing purposes.
	 * 
	 */
	public void removeMembershipTable()
	{
		removeTable(MEMBERSHIP_TABLE);
		return;
	}
	
	/**
	 * Updates the specified entry of the Membership table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param _userid
	 *            The id of the user to be updated
	 * @param _groupMembership
	 *            The new group that the user belongs to
	 * @param _latitude
	 *            The new latitude of the group location
	 * @param _longitude
	 * 			  The new longitude of the group location
	 * @return
	 *        True if succeeds. False otherwise.
	 */
	public boolean updateEntryOfMembershipTable(String _userid, String _groupMembership,
			double _latitude, double _longitude) {
		String query = "update " + MEMBERSHIP_TABLE + " set membership = " 
				     + "'" + _groupMembership + "', latitude = " + Double.toString(_latitude)
				     + ", longitude = " + Double.toString(_longitude) + " where userid = '" + _userid + "'";
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("Error while updating. " + query + " of " + MEMBERSHIP_TABLE);
			return false;
		}
		return true;
	}
	
	/**
	 * Adds the specified entry to the table.
	 * 
	 * <p>
	 * Returns false if table doesn't exist or unsuccessful. True otherwise.
	 * 
	 * @param entry
	 *            The entry to be added
	 * @param tableName
	 *            The table to add the entry to
	 * @return
	 *        True is succeeds. False otherwise.
	 */
	private boolean addEntryToTable(String entry, String tableName) {
		String query = "insert into " + tableName + " values"+ entry;
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("Error while adding " + entry + " to " + tableName);
			return false;
		}
		return true;
	}
	
	/**
	 * Removes an entry from the specified table, if entry exists.
	 * 
	 * <p>
	 * Returns false if table doesn't exist, the entry is missing or 
	 * other general issues occur.
	 * 
	 * @param entry
	 *            Item to be removed
	 * @param tableName
	 *            Table to remove the entry from.
	 */	
	private boolean removeEntryFromTable(String entry, String tableName) {
		String query = "delete from " + tableName+ " where " + entry;
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("Error while removing " + entry + " from " + tableName);
			return false;
		}
		return true;
	}
	
	/**
	 * Creates a new table in the database.
	 * 
	 * <p>
	 * See above
	 * 
	 * @param tableName
	 *            Table name to create.
	 * @param parameters
	 *            The table fields.
	 */
	private void createTable(String tableName, String parameters) {
		String query = "create table " + tableName
				+ " " + parameters;
		if(tableExists(tableName))
		{
			logger.info("createTable " +  tableName + " already exists. Not creating.");
			return;
		}
		
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.severe("Error when creating the table " + tableName + " Params: " + parameters);
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return;
	}
	
	/**
	 * Removes a table from the database.
	 * 
	 * <p>
	 * Removes table.
	 * 
	 * @param tableName
	 *            Table name to remove.
	 */
	private void removeTable(String tableName) {
		String query = "drop table if exists " + tableName;
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.severe("Error when removing the table " + tableName );
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return;
	}

	
	/**
	 * Checks if a specified table exists in the database
	 * 
	 * <p>
	 * Returns true if table exists
	 * 
	 * @param tableName
	 *            Table name to check for.
	 */
	private boolean tableExists(String tableName) {
		String query = "select * from sqlite_master where type='table' AND name='"
				+ tableName + "'";
		try {
			ResultSet rs = statement.executeQuery(query);
			if (rs != null && !rs.next()) {
				logger.warning("Table not found: " + tableName);
				return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}
}
