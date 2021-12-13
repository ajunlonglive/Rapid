package com.rapid.utils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.rapid.core.Application;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.ConnectionAdapter.ConnectionAdapterException;
import com.rapid.data.DataFactory;
import com.rapid.data.DatabaseConnection;
import com.rapid.data.SimpleConnectionAdapter;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class DataFactoryBuilder {

	public static DataFactory createDataFactory(RapidRequest rapidRequest, DatabaseConnection connection) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {

		DataFactory dataFactory = null;
		ServletContext context = null;
		Application application = null;

		if (rapidRequest != null) {

			RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

			if (rapidServlet != null) {
				context = rapidServlet.getServletContext();
				application = rapidRequest.getApplication();
			}

		}

		ConnectionAdapter connectionAdapter = connection.getConnectionAdapter(context, application);
		dataFactory = new DataFactory(connectionAdapter, false);

		return dataFactory;
	}

	public static DataFactory createDataFactory(RapidRequest rapidRequest) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		DatabaseConnection connection = rapidRequest.getApplication().getDatabaseConnections().get(0);
		return createDataFactory(rapidRequest, connection);
	}

	public static DataFactory createDataFactory(RapidRequest rapidRequest, int connectionNumber) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		DatabaseConnection connection = rapidRequest.getApplication().getDatabaseConnections().get(connectionNumber);
		return createDataFactory(rapidRequest, connection);
	}

	public static DataFactory createMasterDataFactory(RapidRequest rapidRequest) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		DataFactory dataFactory = null;

		Application application = rapidRequest.getApplication();
		List<DatabaseConnection> connections = application.getDatabaseConnections();
		DatabaseConnection connection = connections.get(0);
		HttpServletRequest request = rapidRequest.getRequest();
		if(request!=null) {
			ServletContext context = request.getServletContext();
			if(context!=null) {
				String driverClass = connection.getDriverClass();
				String connectionString = connection.getConnectionString();
				String username = connection.getUserName();
				String password = connection.getPassword();
				ConnectionAdapter connectionAdapter = new SimpleConnectionAdapter(context, driverClass, connectionString, username, password);
				dataFactory = new DataFactory(connectionAdapter, false);
			}
		}

		return dataFactory;
	}

	public static DataFactory createMasterDataFactory(ServletContext context, Application application) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		DatabaseConnection connection = application.getDatabaseConnections().get(0);
		return createMasterDataFactory(context, application, connection);
	}

	public static DataFactory createMasterDataFactory(ServletContext context, Application application, ConnectionAdapter connectionAdapter) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		DataFactory dataFactory = new DataFactory(connectionAdapter, false);
		return dataFactory;
	}

	public static DataFactory createMasterDataFactory(ServletContext context, Application application, DatabaseConnection connection) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		String driverClass = connection.getDriverClass();
		String connectionString = connection.getConnectionString();
		String username = connection.getUserName();
		String password = connection.getPassword();
		ConnectionAdapter connectionAdapter = new SimpleConnectionAdapter(context, driverClass, connectionString, username, password);
		DataFactory dataFactory = createMasterDataFactory(context, application, connectionAdapter);
		return dataFactory;
	}

	public static DataFactory checkDataFactoryConnection(RapidRequest rapidRequest, DataFactory dataFactory) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException, ConnectionAdapterException {
		if (dataFactory.getConnection(rapidRequest).isClosed())
			dataFactory = createMasterDataFactory(rapidRequest.getRapidServlet().getServletContext(), rapidRequest.getApplication());
		return dataFactory;
	}
}