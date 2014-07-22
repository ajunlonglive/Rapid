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

//log that this is loading
Logger.getLogger(this.getClass()).debug("design.jsp request : " + request.getQueryString());
//get the applications
Map<String,Application> applications = (Map<String,Application>) getServletContext().getAttribute("applications");
// retain a ref to rapid app
Application rapid = applications.get("rapid");
// get the userName
String userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
// null safety
if (userName == null) userName = "";
// get a rapid request
RapidRequest rapidRequest = new RapidRequest(request); 
// check permission
boolean designerPermission = rapid.getSecurity().checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE);
%>
<html>
<head>
	
	<title>Rapid Desktop - Designer</title>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8">
<%
	if (designerPermission) {
%>		
	<script type="text/javascript" src="scripts/jquery-1.10.2.js"></script>
	<script type="text/javascript" src="scripts/jquery-ui-1.10.3.js"></script>
	<script type="text/javascript" src="scripts/extras.js"></script>
	<script type="text/javascript" src="scripts/designer.js"></script>
	<script type="text/javascript" src="scripts/reorder.js"></script>
	<script type="text/javascript" src="scripts/properties.js"></script>
	<script type="text/javascript" src="scripts/controls.js"></script>
	<script type="text/javascript" src="scripts/validation.js"></script>
	<script type="text/javascript" src="scripts/actions.js"></script>
	<script type="text/javascript" src="scripts/styles.js"></script>
	<script type="text/javascript" src="scripts/dialogue.js"></script>
	<script type="text/javascript" src="scripts/map.js"></script>
	<script type="text/javascript" src="scripts/help.js"></script>			
	<script type="text/javascript" src="applications/rapid/rapid.js"></script>
	<script type="text/javascript">
	
	var _userName = "<%=userName %>";
	
	</script>	
	<link rel="stylesheet" type="text/css" href="styles/designer.css"></link>
<%
	} else {
%>
	<link rel="stylesheet" type="text/css" href="styles/index.css"></link>
<%
	}
%>
</head>
<body>
<%
	if (designerPermission) {
%>	
	<div id="loading">
		<div id="loadingPanel">
			<div><img style="padding: 10px;width: 200px; height:134px; margin-left:-50px;" src="images/RapidLogo_200x134.png" /></div>
			<div><b>Rapid <%=com.rapid.server.Rapid.VERSION %></b></div>		
			<div><img style="margin-top: 5px; margin-bottom: 5px;" src="images/wait_220x19.gif"></div>		
			<div>loading...</div>
		</div>
	</div>
	
	<iframe id="page" scrolling="no"></iframe>
	
	<div id="designerTools">	 
			
		<div id="controlPanelShow" style="z-index:10004"></div>
		
		<div id="controlPanel" style="z-index:10005">
			<div id="controlPanelPin"><img src="images/pinned_14x14.png" title="unpin" /></div>
			<div class="buttons">
				<button id="appAdmin" class="buttonLeft buttonRight" title="Load the Rapid administration screen">administration</button>
			</div>
			
			<h2>Application<img id="helpApplication" class="headerHelp" src="images/help_16x16.png" /></h2>
			<select id="appSelect">
				<!-- Applications are added here as options the designer loads -->
			</select>				
			
			<h2>Page<img id="helpPage" class="headerHelp" src="images/help_16x16.png" /></h2>
			<select id="pageSelect">
				<!-- Pages are added here as options the designer loads -->
			</select>
			
			<div id="pageLock">
				<h3>This page is locked for editing by Gareth Edwards</h3>
			</div>
			
			<div class="buttons">				
				<button id="pageEdit" class="buttonLeft buttonRight" title="View and edit the page properties">properties</button>
			</div>		
							
			<div class="buttons">
				<button id="pageNew" class="buttonLeft" title="Create a new page for this application">new</button>
				<button id="pageSave" class="" title="Save this page">save</button>
				<button id="pageView" class="buttonRight" title="View this page in the application">view</button>
			</div>	
					
			<div class="buttons">
				<button id="undo" class="buttonLeft" disabled="disabled" title="Undo changes">undo</button>
				<button id="redo" class="buttonRight" disabled="disabled" title="Redo changes">redo</button>
			</div>	
							
			<div id="controlControls">
				<h2 id="controlsHeader">Controls
					<img class="headerToggle" src="images/triangleUp_8x8.png" />
					<img id="helpControls" class="headerHelp" src="images/help_16x16.png" />
				</h2>
				
				<ul id="controlsList" class="design-controls" >
					<!-- Controls are added here as list items when the designer loads -->
				</ul>					
			</div>	
			
			<h2 id="controlsMap" style="margin-top:0;">Page controls
				<img class="headerToggle" src="images/triangleUp_8x8.png" />
				<img id="helpMap" class="headerHelp" src="images/help_16x16.png" />
			</h2>
			<div id="pageMap" class="design-map" >
				<!-- The control page is added here when the page has loaded -->
			</div>	
			
			<hr/>
			
			<div class="controlPanelVersion" >
				<img src="images/RapidLogo_60x40.png" style="margin-left:-16px;"/>
				<div id="controlPanelVersion">Rapid<br/><%=com.rapid.server.Rapid.VERSION %></div>
			</div>		
													
		</div>
		
		<div id="propertiesPanel" style="z-index:10005">
		
			<div class="untilsPanelDiv">
				<img id="helpPropertiesPanel" class="headerHelp" src="images/help_16x16.png" />							
				<div class="buttons">					
					<button id="selectPeerLeft" class="buttonLeft"><img src="images/moveLeft_16x16.png" title="Select the control before this one"/></button>
					<button id="selectParent"><img src="images/moveUp_16x16.png" title="Select the parent of this control"/></button>
					<button id="selectChild"><img src="images/moveDown_16x16.png" title="Select the first child of this control"/></button>
					<button id="selectPeerRight" class="buttonRight"><img src="images/moveRight_16x16.png" title="Select the control after this one"/></button>
				</div>													
				<div class="buttons">
					<button id="swapPeerLeft" class="buttonLeft"><img src="images/swapLeft_16x16.png" title="Swap position with control before this one"/></button>
					<button id="addPeerLeft"><img src="images/addLeft_16x16.png" title="Add a new control before this one"/></button>
					<button id="deleteControl">&nbsp;<img src="images/bin_16x16.png" title="Delete this control"/>&nbsp;</button>
					<button id="addPeerRight"><img src="images/addRight_16x16.png" title="Add a new control after this one"/></button>
					<button id="swapPeerRight" class="buttonRight"><img src="images/swapRight_16x16.png" title="Swap position with control after this one"/></button>
				</div>						
				<div class="buttons">
					<button id="copy" class="buttonLeft">&nbsp;copy</button>
					<button id="paste" class="buttonRight">paste</button>
				</div>									
			</div>			
			
			<div class="propertiesPanelDiv"></div>			
			<div class="validationPanelDiv"></div>
			<div id="actionsPanelDiv" class="actionsPanelDiv"></div>
			<div id="stylesPanelDiv"></div>			
						
		</div>
		
		<div id="propertiesDialogues"></div>
		
		<span id="styleInput" contenteditable="true"></span>
		<span id="styleHint"></span>
		<ul id="styleList"></ul>
		
		<div id="selectionCover" style="display:none;position:absolute;left:0px;top:0px;z-index:10000;">
			<div style="background-color:white;width:100%;height:100%;opacity:0.5;" ></div>
		</div>
		
		<div id="selectionBorder">
			<div id="selectionBorderLeft" class="selectionBorderInner"></div>
			<div id="selectionBorderTop" class="selectionBorderInner"></div>
			<div id="selectionBorderRight" class="selectionBorderInner"></div>
			<div id="selectionBorderBottom" class="selectionBorderInner"></div>
		</div>
		
		<img id="selectionMoveLeft" style="display:none;position:absolute;left:0px;top:0px;z-index:10002;" src="images/moveLeft_32x32.png" />
		<img id="selectionMoveRight" style="display:none;position:absolute;left:0px;top:0px;z-index:10002;" src="images/moveRight_32x32.png" />
		<img id="selectionInsert" style="display:none;position:absolute;left:0px;top:0px;z-index:10002;" src="images/insert_32x32.png" />
	
		<div id="designCover"></div>
											
	</div>	
	
	<iframe id="uploadIFrame" name="uploadIFrame" width="0" height="0" style="width:0;height:0;border:0px hidden #fff;" onload="fileuploaded(this);"></iframe>
	
	<div id="dialogues"></div>

<%
	} else {
%>

	<div class="image">
		<img src="images/RapidLogo_200x134.png" />
	</div>
	
	<div class="title">
		<span>Rapid - version <%=com.rapid.server.Rapid.VERSION %></span>
	</div>

	<center><h3>You do not have permission to access the Rapid Designer</h3></center>
		
<%		
	}
%>			
</body>
</html>