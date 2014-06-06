<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.filter.*" %>
<%@ page import="com.rapid.server.RapidHttpServlet.RapidRequest" %>
<%

//get the applications
Map<String,Application> applications = (Map<String,Application>) getServletContext().getAttribute("applications");
// get the rapid app
Application rapid = applications.get("rapid");
// get the userName
String userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
// null safety
if (userName == null) userName = "";
//get a rapid request
RapidRequest rapidRequest = new RapidRequest(request); 
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	
	<title>Rapid - Welcome</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	<script type="text/javascript" src="scripts/jquery-1.10.2.js"></script>
	<script type="text/javascript">
	
function loadApps() {
	
	var appsDiv = $("#apps");
	
	$.ajax({
    	url: "rapid?a=rapid&action=getApps",
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
	<img src="images/administration_157x135.png" /><a href="~?a=rapid&p=P0">Administration</a>
</div>
<% 
	}
	if (rapid.getSecurity().checkUserRole(rapidRequest, userName, "RapidDesign")) {
%>
<div class="body">
	<img src="images/designer_157x135.png" /><a href="design.jsp">Design</a>
</div>
<% 
	}
%>

<div class="body">
	<img src="images/application_157x135.png" /><a href="#" onclick="loadApps();">Applications</a>
</div>

<div class="apps" id="apps">
</div>

<div class="info">
	<p><a href="logout.jsp">Log out</a></p>	
</div>

</body>
</html>