package com.rapid.data;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.rapid.data.ConnectionAdapter.ConnectionAdapterException;
import com.rapid.server.RapidHttpServlet.RapidRequest;

public class DataFactory {
	
	public static class Parameter {
		
		public static final int NULL = 1;
		public static final int STRING = 2;
		public static final int DATE = 3;
		public static final int INTEGER = 4;		
		public static final int FLOAT = 5;
		
		private int _type;
		private String _string;
		private Date _date;
		private int _int;		
		private float _float;
		
		public Parameter() {
			_type = NULL;
		}
		
		public Parameter(String value) {
			_type = STRING;
			_string = value;
		}
		
		public Parameter(Date value) {
			_type = DATE;
			_date = value;
		}
		
		public Parameter(int value) {
			_type = INTEGER;
			_int = value;
		}
		
		public Parameter(float value) {
			_type = FLOAT;
			_float = value;
		}
		
		public int getType() { return _type; }
		public String getString() { return _string; }
		public Date getDate() { return _date; }
		public int getInteger() { return _int; }		
		public float getFloat() { return _float; }
		
	}
		
	@SuppressWarnings("serial")
	public static class Parameters extends ArrayList<Parameter> {
		
		public void addNull() { this.add(new Parameter()); }
		public void addString(String value) { this.add(new Parameter(value)); }
		public void addInt(int value) { this.add(new Parameter(value)); }
		public void addDate(Date value) { this.add(new Parameter(value)); }
		public void addFloat(float value) { this.add(new Parameter(value)); }
		public void add() { this.add(new Parameter()); }
		public void add(String value) { this.add(new Parameter(value)); }
		public void add(int value) { this.add(new Parameter(value)); }
		public void add(Date value) { this.add(new Parameter(value)); }
		public void add(float value) { this.add(new Parameter(value)); }
		
	}
			
	private ConnectionAdapter _connectionAdapter;
	private String _sql;
	private boolean _autoCommit, _readOnly;
	private Connection _connection; 	
	private Statement _statement;
	private PreparedStatement _preparedStatement;
	private ResultSet _resultset;
		
	public DataFactory(ConnectionAdapter connectionAdapter) {
		_connectionAdapter = connectionAdapter;
		_autoCommit = true;
	}
	
	public DataFactory(ConnectionAdapter connectionAdapter, boolean autoCommit) {
		_connectionAdapter = connectionAdapter;
		_autoCommit = autoCommit;
	}
	
	public Connection getConnection(RapidRequest rapidRequest) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
			
		_connection = _connectionAdapter.getConnection(rapidRequest);
		
		_connection.setAutoCommit(_autoCommit);
		
		_connection.setReadOnly(_readOnly);
							
		return _connection;
				
	}
	
	public boolean getReadOnly(boolean readOnly) { return _readOnly; }	
	public void setReadOnly(boolean readOnly) {	_readOnly = readOnly; }
						
	public PreparedStatement getPreparedStatement(RapidRequest rapidRequest, String SQL, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException  {
		
		_sql = SQL;
		
		if (_connection == null) _connection = getConnection(rapidRequest);
		
		//if (_connection.isClosed()) _connection = getConnection(rapidRequest);
		
		//if (_resultset != null) _resultset.close();
		
		//if (_preparedStatement != null) _preparedStatement.close();	
		
		_preparedStatement = _connection.prepareStatement(_sql);
		
		ParameterMetaData parameterMetaData = _preparedStatement.getParameterMetaData();
										
		if (parameters == null) {
			
			if (parameterMetaData.getParameterCount() > 0) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, none provided");
			
		} else {
			
			if (parameterMetaData.getParameterCount() != parameters.size()) throw new SQLException("SQL has " + parameterMetaData.getParameterCount() + " parameters, " + parameters.size() + " provided");
		
			int i = 0;
			
			for (Parameter parameter : parameters) {
			
				i++;
				
				switch (parameter.getType()) {
				case Parameter.NULL : _preparedStatement.setNull(i, java.sql.Types.NULL); break;
				case Parameter.STRING : 											
					if (parameter.getString() == null) {
						_preparedStatement.setNull(i, java.sql.Types.NULL);
					} else {
						_preparedStatement.setString(i, parameter.getString());
					}
					break;
				case Parameter.DATE : 				
					if (parameter.getDate() == null) {
						_preparedStatement.setNull(i, java.sql.Types.NULL);
					} else {
						_preparedStatement.setTimestamp(i, new Timestamp(parameter.getDate().getTime()));
					}
					break;
				case Parameter.INTEGER : _preparedStatement.setInt(i, parameter.getInteger()); break;
				case Parameter.FLOAT : _preparedStatement.setFloat(i, parameter.getFloat()); break;
				}						
			}
		
		}
		
		return _preparedStatement;
		
	}
	
	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String SQL, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
				
		_resultset = getPreparedStatement(rapidRequest, SQL, parameters).executeQuery();
		
		return _resultset;
				
	}
	
	public ResultSet getPreparedResultSet(RapidRequest rapidRequest, String SQL) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
		
		_resultset = getPreparedStatement(rapidRequest, SQL, null).executeQuery();
		
		return _resultset;
				
	}
	
	public int getPreparedUpdate(RapidRequest rapidRequest,String SQL, ArrayList<Parameter> parameters) throws SQLException, ClassNotFoundException, ConnectionAdapterException {
		
		return getPreparedStatement(rapidRequest, SQL, parameters).executeUpdate();
		
	}	
	
	public void commit() throws SQLException {
		
		if (_connection != null) _connection.commit();
		
	}
	
	public void rollback() throws SQLException {
		
		if (_connection != null) _connection.rollback();
		
	}
	
	public void close() throws SQLException {
		
		//if (_statement != null) _statement.close();
		
		//if (_preparedStatement != null) _preparedStatement.close();
						
		//if (_resultset != null) _resultset.close();				
		
		if (_connection != null) _connectionAdapter.closeConnection(_connection);
		
	}

}
