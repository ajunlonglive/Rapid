<%@page import="com.rapid.server.Rapid"%><%@page import="com.rapid.core.Application"%><%@page import="com.rapid.core.Applications"%><%@page import="com.rapid.core.Page"%><%@page import="com.rapid.core.Pages"%><%@page import="com.rapid.server.RapidRequest"%><%@page import="com.rapid.server.RapidHttpServlet"%><%@page import="com.rapid.security.SecurityAdapter"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<head>
<link rel="stylesheet" type="text/css" href="styles/fonts/fontawesome/css/font-awesome.css">
<style>
body {
	margin: 0;
	background: #333;
	font-family: sans-serif;
	color: #DDD;
	overflow: hidden;
}

h1 {
	margin: 0;
	display: inline-block;
	font-size: 2em;
	color: #EEE;
}

h1 .version {
	font-weight:400;
	color:#AAA;
}

header {
	padding: 1em;
}

#pagesGroup {
	overflow-y: overlay;
	height: calc(100vh - 4.2rem);
	scroll-snap-type: y mandatory;
	font-size: 100vw;
}

.pageView {
	display: inline-block;
	width: 1em;
	height: 1em;
	max-height: 100%;
	padding: 1rem;
	padding-top: 0;
	box-sizing: border-box;
	scroll-snap-align: start;
}

.pageView .pageContainer {
	width: 100%;
	height: calc(100% - 3rem);
	overflow: hidden;
}

.pageView .page {
	width: 100%;
	height: 100%;
	transform: scale(1) translate(0, 0);
	border: none;
	background: #FFF;
}

.pageView .name {
	font-size: 1.1rem;
	margin: 0;
	white-space: nowrap;
	display: inline-block;
	max-width: calc(100% - 4em);
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
		out.write("<h1>" + app.getTitle() + " <span class='version'>" + app.getVersion() + "</span></h1>");
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

setInterval(function() {
	document.body.focus();
}, 1000);

var pages = document.querySelectorAll(".page");
var pagesArray = [];
for (var i = 0; i < pages.length; i++) pagesArray.push(pages[i]);

var scale = 1;
document.body.addEventListener("wheel", function(wheel) {
	if (wheel.shiftKey) {
		if (wheel.deltaY < 0) {
			// in
			scale = Math.min(1, scale * Math.pow(1.001, Math.abs(wheel.deltaY)));
		} else {
			// out
			scale = Math.max(0.1, scale / Math.pow(1.001, Math.abs(wheel.deltaY)));
		}
		var rounded = Math.round(scale * 10e3) / 10e3;
		pagesGroup.style.fontSize = (rounded * 100) + "vw";
		var pc = (1 / rounded) * 100;
		var pageScale = pc + "%";
		var trans = (pc - 100) / 2;
		pagesArray.forEach(function(page) {
			var style = page.style;
			style.width = pageScale;
			style.height = pageScale;
			style.transform = "scale(" + rounded + ") translate(-" + trans + "%, -" + trans + "%)";
		});
	}
});

pagesArray.forEach(function(page) {
	page.addEventListener("load", function() {
		page.contentWindow.document.body.addEventListener("click", function() {
			window.focus();
		});
	});
});

</script>