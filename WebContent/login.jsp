<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%

String message = (String) session.getAttribute("Message");

%>
<html>
<head>
	
	<title>Rapid - Log in</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
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
				<td colspan="2" style="text-align:right;"><input type="submit"/ value="log in"></td>
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