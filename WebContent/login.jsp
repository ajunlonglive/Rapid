<!DOCTYPE html><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%@page import="com.rapid.server.RapidRequest"%><%@page import="com.rapid.server.filter.RapidFilter"%><%

/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

// retieve any message from the session - usually that user/password has failed
String message = (String) session.getAttribute("message");

// if the main filter supports logon
if (RapidFilter.hasLogon()) {

%>
<html>
<head>
	<title>Rapid - Log in</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	<link rel='stylesheet' type='text/css' href='styles/fonts/fontawesome/css/font-awesome.css'></link>
</head>

<body onload="document.login.userName.focus();">

<div class="image">
	<a href="http://www.rapid-is.co.uk"><img title="Rapid Information Systems" src="images/RapidLogo-small.svg" /></a>	
</div>

<div class="midTitle" style="">
	<span style="">Rapid</span>
</div>
<div class="subBar">
	<div class="versionColumn"><%=com.rapid.server.Rapid.VERSION %></div>
</div>

<div class="body">

	<div class="columnMiddle">

		<form name="login" id="RapidLogin" method="post">
		
			<div class="row">
				<div class="columnUserInput">
					<div class="columnUserIcon" style="">
						<span class="fa fa-user" style=""></span>
					</div>
					<input type="text" placeholder="Username" name="userName" autocomplete="username" required="required">
				</div>
			</div>
			
			<div class="row">
				<div class="columnUserIcon"><span class="fa fa-lock"></span></div>
				<div class="columnUserInput"><input type="password" placeholder="Password" name="userPassword" autocomplete="current-password"></div>
			</div>
			<input type='hidden' name='csrfToken' value='<%=RapidRequest.getCSRFToken(session) %>' />
			<button type="submit"><i class="fas fa-sign-in-alt"></i>Log in</button>
			
		</form>
				
<% 
	if (message != null) {
%>
	<p class="message"><%=message %></p>
<%	
	// empty the message
		session.setAttribute("message", null);
	}
%>
	</div>	
	
</div>

</body>
</html>
<% 
} else {

	// redirect the user to the index page
	response.sendRedirect(".");
	
}
%>