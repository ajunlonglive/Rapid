<%@page import="com.rapid.server.Rapid"%><%@page import="com.rapid.core.Application"%><%@page import="com.rapid.core.Applications"%><%@page import="com.rapid.core.Page"%><%@page import="com.rapid.core.Pages"%><%@page import="com.rapid.server.RapidRequest"%><%@page import="com.rapid.server.RapidHttpServlet"%><%@page import="com.rapid.security.SecurityAdapter"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<head>
<link rel="stylesheet" type="text/css" href="styles/fonts/fontawesome/css/font-awesome.css">
<style>
body {
	margin: 0;
	background: #333;
	font-family: sans-serif;
	color: #DDD;
	transition: background 0.25s ease-in-out, color 0.25s ease-in-out;
}

.nativeScale {
	background: #111;
	color: #AAA;
}

h1 {
	margin: 0;
	display: inline-block;
	font-size: 2em;
	color: #EEE;
	transition: color 0.25s ease-in-out;
}

.nativeScale h1 {
	color: #AAA;
}

header {
	padding: 1em;
}

.pageView {
	display: inline-block;
	width: 350px;
	height: 350px;
	max-height: 90vh;
	padding: 0 1em 1.5em;
	box-sizing: border-box;
	transition: width 0.25s ease-in-out, height 0.25s ease-in-out, padding 0.25s ease-in-out;
}

.singleView .pageView {
	width: 100%;
	height: 95%;
}

.pageView .pageContainer {
	width: 100%;
	height: calc(100% - 2em);
	overflow: hidden;
	box-shadow: 0 0 0 0.05em #ffffff88;
}

.singleView .pageContainer {
	height: calc(100% - 2em);
}

.pageView .page {
	width: 400%;
	height: 400%;
	transform: scale(0.25) translate(-150%, -150%);
	border: none;
	background: #FFF;
}

.pageView .name {
	font-size: 1.1em;
	margin: 0;
	white-space: nowrap;
	display: inline-block;
	max-width: calc(100% - 4em);
	overflow: hidden;
}

.pageView .label {
	display: block;
	margin-top: 0.5em;
}

.action {
	float: right;
	color: #DDD;
	background: #444;
	border: none;
	margin-left: 0.2em;
	margin-bottom: 0.2em;
	width: 2.5em;
	height: 2em;
	cursor: pointer;
	font-size: 0.75em;
	padding: 0;
}

.action:hover {
	color: #FFF;
	background: #555;
}

header .action {
	font-size: 1em;
	width: 2em;
}

</style>
</head>
<%

// retrieves a specified application page and returns it as the response to requesting this .jsp

// get the servletContext
final ServletContext servletContext = getServletContext();

// retieve the applications collection
Applications applications = (Applications) servletContext.getAttribute("applications");

// retrieve the application we want
String a = request.getParameter("a");
String v = request.getParameter("v");
Application app = applications.get(a, v);
if (app != null) {
	SecurityAdapter security = app.getSecurityAdapter();
	RapidHttpServlet rapidServlet = new RapidHttpServlet() {
		@Override
		public ServletContext getServletContext() {
			return servletContext;
		}
	};
	
	RapidRequest rapidRequest = new RapidRequest(rapidServlet, request);
	
	if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
		
		Pages pages = app.getPages();
		Pages.PageHeaders sortedPagesHeaders = pages.getSortedPages();
		out.write("<header>");
		out.write("<h1>" + app.getTitle() + " - " + app.getVersion() + "</h1>");
		out.write("<a href='~?a=rapid&appId=" + a + (v != null ? "&version=" + v : "") + "'><button type='button' class='action'><i class='fa fa-cog' aria-hidden='true'></i></button></a>");
		out.write("</header>");
		out.write("<div>");
		for (Pages.PageHeader pageHeader : sortedPagesHeaders) {
			String p = pageHeader.getId();
			String pageName = pageHeader.getName();
			String pageTitle = pageHeader.getTitle();
			out.write("<div class='pageView'>");
			out.write("<div class='pageContainer'>");
			out.write("<iframe class='page' src='pagePreview.jsp?a=" + a + "&p=" + p + "'></iframe>");
			out.write("</div>");
			out.write("<div class='label'>");
			out.write("<p class='name'>" + pageTitle + "</p>");
			out.write("<a href='~?a=" + a + (v != null ? "&v=" + v : "") + "&p=" + p + "' target='blank'><button type='button' class='action'><i class='fa fa-play' aria-hidden='true'></i></button></a>");
			out.write("<a href='design.jsp?a=" + a + (v != null ? "&v=" + v : "") + "&p=" + p + "'><button type='button' class='action'><i class='fa fa-wrench' aria-hidden='true'></i></button></a>");
			out.write("</div>");
			out.write("</div>");
			out.flush();
		}
		out.write("</div>");
	}
}
%>
<script>

var setPageSize = function() {
	document.body.style.fontSize = "calc(1em / " + window.devicePixelRatio + ")";
	if (window.devicePixelRatio === 4) {
		document.body.classList.add("nativeScale");
	} else {
		document.body.classList.remove("nativeScale");
	}
	if (window.devicePixelRatio >= 2.5) {
		document.body.classList.add("singleView");
	} else {
		document.body.classList.remove("singleView");
	}
};

addEventListener("resize", function(ev) {
	setTimeout(setPageSize);
});

setTimeout(setPageSize);

</script>