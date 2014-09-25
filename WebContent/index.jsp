<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.filter.*" %>
<%@ page import="com.rapid.server.RapidRequest" %>
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

//get the applications
Applications applications = (Applications) getServletContext().getAttribute("applications");
// get the rapid app
Application rapid = applications.get("rapid");
// get the userName
String userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
// null safety
if (userName == null) userName = "";
//get a rapid request
RapidRequest rapidRequest = new RapidRequest(request); 
%>
<!DOCTYPE html>
<html>
<head>
	
	<title>Rapid - Welcome</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">	
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	<script type="text/javascript" src="scripts/jquery-1.10.2.js"></script>
	<script type="text/javascript">
	
function loadApps() {
	
	var appsDiv = $("#apps");
	
	$.ajax({
    	url: "~?action=getApps",
    	type: "POST",          
    	dataType: "json",    
    	data: "{}",
        error: function(server, status, error) { 
        	appsDiv.html(error); 
        },
        success: function(data) {
        	if (data && data.length > 0) {
        		// build a list with links that the user has access to
        		var appHtml = "<ul>";
        		for (var i in data) {
        			var app = data[i];
        			appHtml += "<li><a href='~?a=" + app.id + "'>" + app.title + "</a></li>";
        		}
        		appHtml += "</ul>";
        		appsDiv.html(appHtml); 
        		// scroll to the bottom of the document to show the list of applications
        		$(window).scrollTop($(document).height());
        	} else {
        		appsDiv.html("You do not have permission to use any applications"); 
        	}   		       	
        }
	});
	
}	
	
	</script>
	
	
</head>
<body>
<div class="image">
	<img src="images/RapidLogo_200x134.png" />
</div>

<div class="title">
	<span>Rapid - version <%=com.rapid.server.Rapid.VERSION %></span>
</div>

<% 
	if (rapid.getSecurity().checkUserRole(rapidRequest, userName, "RapidAdmin")) {
%>
<div class="body">
	<a href="~?a=rapid"><img src="images/administration_157x135.png" /><span id="admin">Admin</span></a>
</div>
<% 
	}
	if (rapid.getSecurity().checkUserRole(rapidRequest, userName, "RapidDesign")) {
%>
<div class="body">
	<a href="design.jsp"><img src="images/designer_157x135.png" /><span id="design">Design</span></a>
</div>
<% 
	}
%>

<div class="body">
	<a href="#" onclick="loadApps();"><img src="images/application_157x135.png" /><span>Applications</span></a>
</div>

<div class="apps" id="apps">
</div>

<div class="info">
	<p><a href="logout.jsp">Log out</a></p>	
</div>

</body>
</html>