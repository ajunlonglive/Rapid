<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd" encoding="utf-8">
<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.Rapid" %>
<%@ page import="com.rapid.server.RapidHttpServlet.RapidRequest" %>
<%@ page import="com.rapid.server.filter.*" %>
<%

/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

// log that this is loading
Logger.getLogger(this.getClass()).debug("designpage.jsp request : " + request.getQueryString());
// retain a ref to the app
Application rapidApp = null;
// retain a ref to the page
Page rapidPage = null;
// retain whether we got an app and a pge
boolean gotAppAndPage = false;
// retain whether we have permission
boolean designerPermission = false;
// get the app parameter
String appId = request.getParameter("a");
//get the page parameter
String pageId = request.getParameter("p");

// check we have both an app and a page
if (appId != null && pageId != null) {
	
	//get the applications
	Map<String,Application> applications = (Map<String, Application>) getServletContext().getAttribute("applications");
	// get the app
	rapidApp = applications.get(appId);
	// get the page
	rapidPage = rapidApp.getPage(pageId);
	// get the userName
	String userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);	
	// null safety
	if (userName == null) userName = "";	
	
	// we got an app and a page
	gotAppAndPage = true;	
	// get a rapid request
	RapidRequest rapidRequest = new RapidRequest(request); 
	// check we have the RapidDesign permission in the security provider for this app
	designerPermission = rapidApp.getSecurity().checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE);
			
}
%>

<html>
<head>	
	<title>Rapid Desktop - Design Page</title>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8">	
	<script type='text/javascript' src='scripts/jquery-1.10.2.js'></script>
	<script type='text/javascript' src='scripts/extras.js'></script>
	<script type='text/javascript' src='scripts/json2.js'></script>
<%

if (gotAppAndPage && designerPermission) {

	for (String resource : rapidApp.getResourceIncludes()) {	
		out.print("\t" + resource + "\n");	
	}
%>
	<link rel="stylesheet" type="text/css" href="applications/<%=rapidApp.getId()%>/rapid.css"></link>
	<link rel="stylesheet" type="text/css" href="applications/<%=rapidApp.getId()%>/<%=rapidPage.getName()%>.css"></link>
	<style type="text/css">
		
	.nonVisibleControl {
		position: fixed;
		bottom: 10px;	
	}
	
	</style>
<%
}
%>	
</head>
<body>
<%
if (!gotAppAndPage) {
%>
	<center><h3>To load the resources an application and a page must be provided</h3></center>
<%
}
if (!designerPermission) {
%>
	<center><h3>You do not have permission to access the Rapid Designer - contact your administrator</h3></center>	
<%
}
%>
</body>
</html>