package Utils;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class PostgreSQLConnector {

	private Connection mConnect = null;
	private Statement mStatement = null;
	public String mDatabase = "";

	public Connection getConnection() {
		return mConnect;
	}

	public PostgreSQLConnector(String user, String password, String database, String[] tables)
			throws SQLException, ClassNotFoundException {
		tryCreatingDatabase(database, user, password);
		// TODO Auto-generated constructor stub
		String url = "jdbc:postgresql://localhost/" + database;

		Class.forName("org.postgresql.Driver");
		mConnect = DriverManager.getConnection(url, user, password);
		// statements allow to issue SQL queries to the database
		mStatement = mConnect.createStatement();
		mDatabase = database;
		tryCreatingTables(tables);
	}

	public void tryCreatingDatabase(String database, String user,
			String password) throws ClassNotFoundException{
		Connection connection = null;
		Statement statement = null;
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(
					"jdbc:postgresql://localhost/", user, password);
			statement = connection.createStatement();
			String sql = "CREATE DATABASE " + database;
			statement.executeUpdate(sql);
			System.out.println("Database created!");
			System.out.println("Creating tables");
			
		} catch (SQLException sqlException) {
			if (sqlException.getErrorCode() == 1007) {
				// Database already exists error
				// System.out.println(sqlException.getMessage());
			} else {
				// Some other problems, e.g. Server down, no permission, etc
				// sqlException.printStackTrace();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void tryCreatingTables(String[] tables){
		for(String sqlCreateTable : tables){
			try {
				mStatement.execute(sqlCreateTable);
				System.out.println(sqlCreateTable);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
	}

	// if you dont have any condition, just pass null into it
	public ResultSet select(String table, String fields[], String condition)
			throws SQLException {
		String field = fields[0];
		for (int i = 1; i < fields.length; i++) {
			field = field + "," + fields[i];
		}
		if (condition == null) {
			return mStatement
					.executeQuery("select " + field + " from  " + table);
		} else {
			return mStatement.executeQuery("select " + field + " from  " + table
					+ " WHERE " + condition);
		}
	}

	public int update(String table, String updatefield, String condition)
			throws SQLException {
		// preparedStatements can use variables and are more efficient
		if (condition != null) {
			int id = mStatement.executeUpdate("update " + table + " set  "
					+ updatefield + " WHERE " + condition);
			return id; // only return the row count
		} else {
			return 0;
		}
	}

	// this is for flexibility
	public ResultSet select(String query) throws SQLException {
		return mStatement.executeQuery("select " + query);
	}

	// values: an array of values, must be in the same order as in the table
	// all values are String
	// arrays: same length with values, indicate the type of values
	// 0 - string
	// 1 - int
	// 2 - long
	// 3 - int[]
	// 4 - long[]
	// 5 - text[]
	// 6 - text[][]
	// 7 - bool
	public int insert(String table, String values[], int[] arrays, boolean ID,
			boolean returningID) throws SQLException {
		// preparedStatements can use variables and are more efficient
		String dumbValues = "";
		String prefix = "";
		for (int i = 0; i < values.length; i++) {
			dumbValues += prefix + "?";
			prefix = ",";
		}
		PreparedStatement preparedStatement = null;
		if (ID) {
			if (returningID) {
				preparedStatement = mConnect.prepareStatement(
						"insert into  " + table + " values (default,"
								+ dumbValues + ")",
						Statement.RETURN_GENERATED_KEYS);
			} else {
				System.out.println(dumbValues);
				preparedStatement = mConnect.prepareStatement("insert into  "
						+ table + " values (default," + dumbValues + ")");
			}
		} else {
			if (returningID) {
				preparedStatement = mConnect.prepareStatement(
						"insert into  " + table + " values ("
								+ dumbValues + ")",
						Statement.RETURN_GENERATED_KEYS);
			} else {
				System.out.println(dumbValues);
				preparedStatement = mConnect.prepareStatement("insert into  "
						+ table + " values (" + dumbValues + ")");
			}
		}
		// parameters start with 1
		Array myArray;
		for (int i = 0; i < values.length; i++) {
			switch (arrays[i]) {
			case 0:
				preparedStatement.setString(i + 1, values[i]);
				break;

			case 1:
				int intValue = Integer.parseInt(values[i]);
				preparedStatement.setInt(i + 1, intValue);
				break;

			case 2:
				long longValue = Long.parseLong(values[i]);
				preparedStatement.setLong(i + 1, longValue);
				break;

			case 3:
				Integer[] argInt = new Integer[0];
				if (!values[i].equals("")) {
					String[] ints = values[i].split(",");
					argInt = new Integer[ints.length];
					for (int j = 0; j < argInt.length; j++) {
						argInt[j] = Integer.parseInt(ints[j]);
					}
				}
				myArray = mConnect.createArrayOf("integer", argInt);
				preparedStatement.setArray(i + 1, myArray);
				break;

			case 4:
				Long[] argLong = new Long[0];
				if (!values[i].equals("")) {
					String[] bigints = values[i].split(",");
					argLong = new Long[bigints.length];
					for (int j = 0; j < argLong.length; j++) {
						argLong[j] = Long.parseLong(bigints[j]);
					}
				}
				myArray = mConnect.createArrayOf("bigint", argLong);
				preparedStatement.setArray(i + 1, myArray);
				break;

			case 5:

				String[] texts = new String[0];
				if (!values[i].equals("")) {
					texts = values[i].split(",");
				}
				myArray = mConnect.createArrayOf("text", texts);
				preparedStatement.setArray(i + 1, myArray);
				break;
			case 6:

				Integer[][] argIntAray = new Integer[0][];
				if (!values[i].equals("")) {
					// "1,2,3;1,2,3;1,2,3"
					String[] intArray = values[i].split(";");
					argIntAray = new Integer[intArray.length][];
					for (int j = 0; j < argIntAray.length; j++) {
						String[] arr = intArray[j].split(",");
						argIntAray[j] = new Integer[arr.length];
						for (int k = 0; k < arr.length; k++) {
							argIntAray[j][k] = Integer.parseInt(arr[k]);
						}
					}
				}
				myArray = mConnect.createArrayOf("integer", argIntAray);
				preparedStatement.setArray(i + 1, myArray);
				break;
			case 7:

				boolean boolValue = Boolean.parseBoolean(values[i]);
				preparedStatement.setBoolean(i + 1, boolValue);
				break;
			}

		}
		int id = preparedStatement.executeUpdate();
		if (returningID) {
			ResultSet rs = preparedStatement.getGeneratedKeys();
			if (rs != null && rs.next()) {
				id = rs.getInt("ID");
			}
		}
		preparedStatement.close();
		return id; // only return the first
	}

	// you need to close all three to make sure
	public void close() throws SQLException {
		if (mStatement != null) {
			mStatement.close();
		}
		if (mConnect != null) {
			mConnect.close();
		}
	}
}
