/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

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
import com.rapid.server.RapidRequest;

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
		
		@Override
		public String toString() {
			switch (_type) {
			case 1 : return "null";
			case 2 : return _string;
			case 3 : return _date.toString();
			case 4 : return Integer.toString(_int);
			case 5 : return Float.toString(_float);
			}
			return "unknown type";
		}
		
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
		
		@Override
		public String toString() {
			String parametersString = "";
			for (int i = 0; i < this.size(); i++) {
				Parameter parameter = this.get(i);
				parametersString += "'" + parameter.toString() + "'";
				if (i < this.size() - 1) parametersString += ", ";
			}
			return parametersString;
		}
		
	}
			
	private ConnectionAdapter _connectionAdapter;
	private String _sql;
	private boolean _autoCommit, _readOnly;
	private Connection _connection; 	
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
		
		// some jdbc drivers need the line breaks in the sql replacing - here's looking at you MS SQL!
		_sql = SQL.replace("\n", " ");
		
		if (_connection == null) _connection = getConnection(rapidRequest);
				
		if (_preparedStatement != null) _preparedStatement.close();	
		
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
				
		if (_preparedStatement != null) _preparedStatement.close();

		if (_connection != null) _connectionAdapter.closeConnection(_connection);
		
	}

}
