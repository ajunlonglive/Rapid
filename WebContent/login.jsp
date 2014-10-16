<!DOCTYPE html encoding="utf-8">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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

String message = (String) session.getAttribute("message");

%>
<html>
<head>
	
	<title>Rapid - Log in</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
	<link rel="icon" href="favicon.ico"></link>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
	
</head>

<body onload="document.login.userName.focus();">

<div class="image">
	<img src="images/RapidLogo_200x134.png" />
</div>

<div class="title">
	<span>Rapid - version <%=com.rapid.server.Rapid.VERSION %></span>
</div>

<div class="body">

	<form name="login">
		<table>
			<tr>
				<td>User name</td><td><input name="userName" /></td>
			</tr>
			<tr>
				<td>Password</td><td><input name="userPassword" type="password" /></td>
			</tr>
			<tr>
				<td colspan="2" style="text-align:right;"><button type="submit">Log in</button></td>
			</tr>		
		</table>
	</form>
	
<% 
if (message != null) {
%>
			<p><%=message %></p>
<%	
}
%>	
	
</div>

</body>
</html>