<%@page import="com.rapid.server.Rapid"%><%@page import="com.rapid.core.Application"%><%@page import="com.rapid.core.Applications"%><%@page import="com.rapid.core.Page"%><%@page import="com.rapid.core.Pages"%><%@page import="com.rapid.server.RapidRequest"%><%@page import="com.rapid.server.RapidHttpServlet"%><%@page import="com.rapid.security.SecurityAdapter"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<head>
<link rel="stylesheet" type="text/css" href="styles/fonts/fontawesome/css/font-awesome.css">
<style>
body {
	margin: 0;
	padding: 0.5em;
	background: #333;
	font-family: sans-serif;
	color: #DDD;
	overflow: overlay;
}

h1 {
	margin: 0;
	display: inline-block;
	font-size: 2em;
	color: #EEE;
}

header {
	padding: 1em;
}

.pageView {
	display: inline-block;
	width: 20em;
	height: 25em;
	max-height: 100%;
	max-width: 100%;
	padding: 0 1rem 1.5rem;
	box-sizing: border-box;
	transition: width 0.25s linear, height 0.25s linear, padding 0.25s linear;
}

.pageView .pageContainer {
	width: 100%;
	height: calc(100% - 2em);
	overflow: hidden;
	box-shadow: 0 0 0 0.05rem #ffffff88;
}

.pageView .page {
	width: 400%;
	height: 400%;
	transform: scale(0.25) translate(-150%, -150%);
	border: none;
	background: #FFF;
	transition: width 0.25s linear, height 0.25s linear, transform 0.25s linear;
}

.pageView .name {
	font-size: 1.1rem;
	margin: 0;
	white-space: nowrap;
	display: inline-block;
	max-width: calc(100% - 4rem);
	overflow: hidden;
}

.pageView .label {
	display: block;
	margin-top: 0.5rem;
}

.action {
	float: right;
	color: #DDD;
	background: #444;
	border: none;
	margin-left: 0.2rem;
	margin-bottom: 0.2rem;
	width: 2.5em;
	height: 2em;
	cursor: pointer;
	font-size: 0.75rem;
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

.shiftKey iframe {
	pointer-events: none;
}

body.shiftKey {
	overflow: hidden;
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
		out.write("<div id='pagesGroup'>");
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

addEventListener("keydown", function(keydown) {
	if (keydown.shiftKey) {
		document.body.classList.add("shiftKey");
	}
});

addEventListener("keyup", function(keyup) {
	document.body.classList.remove("shiftKey");
});

var pages = document.querySelectorAll(".page");

var size = 0;
document.body.addEventListener("wheel", function(scroll) {
	if (scroll.shiftKey) {
		var direction = -Math.sign(scroll.deltaY);
		size = Math.min(1, Math.max(0, size + (direction * 0.1)));
		console.log("size: " + size);
		var scale = Math.pow(1 + size, 2);
		pagesGroup.style.fontSize = scale + "em";
		
		var counterScale = 0.25 * scale;
		var pc = (1 / counterScale) * 100;
		var pageScale = pc + "%";
		var trans = (pc - 100) / 2;
		for (var i = 0; i < pages.length; i++) {
			var style = pages[i].style;
			style.width = pageScale;
			style.height = pageScale;
			style.transform = "scale(" + counterScale + ") translate(-" + trans + "%, -" + trans + "%)";
		}
	}
});

</script>