<%@page import="com.rapid.core.Application"%><%@page import="com.rapid.core.Applications"%><%@page import="com.rapid.core.Page"%><%@page import="com.rapid.server.RapidRequest"%><%@page import="com.rapid.server.RapidHttpServlet"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%

// retrieves a specified application page and returns it as the response to requesting this .jsp

// get the servletContext
final ServletContext servletContext = getServletContext();

// retieve the applications collection
Applications applications = (Applications) servletContext.getAttribute("applications");

// retrieve the application we want
String a = request.getParameter("a");
String v = request.getParameter("v");
Application app = applications.get(a, v);

// retrieve the page we want
String p = request.getParameter("p");
Page rapidPage = app.getPages().getPage(servletContext, p);

// create an anomyous RapidHttpServlet which returns the servletContext we got earlier - we need this to make a Rapid request
RapidHttpServlet rapidServlet = new RapidHttpServlet() {
	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}
};

// make a RapidRequest with the anomymous RapidHttpServlet and original http request to the .jsp
RapidRequest rapidRequest = new RapidRequest(rapidServlet, request);

// use the page writeHtml with the .jsp out PrintWriter to print the page (could also make a StringWriter and print to that if we want to replace anything)
rapidPage.writeHtml(rapidServlet, response, rapidRequest, app, null, out, false, false, false);
%>