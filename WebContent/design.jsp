<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd" encoding="utf-8">
<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="com.rapid.core.*" %>
<%@ page import="com.rapid.server.Rapid" %>
<%@ page import="com.rapid.server.RapidHttpServlet.RapidRequest" %>
<%@ page import="com.rapid.server.filter.*" %>
<%

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
	<script type="text/javascript" src="applications/rapid/rapid.js"></script>
	
	<link rel="stylesheet" type="text/css" href="styles/designer.css"></link>
<%
	}
%>
</head>
<body>
<%
	if (designerPermission) {
%>	
	<iframe id="page"></iframe>
	
	<div id="designerTools">	 
			
		<div id="controlPanelShow" style="z-index:10004"></div>
		
		<div id="controlPanel" style="z-index:10005">
			<div id="controlPanelPin"><img src="images/pinned_14x14.png" title="unpin" /></div>
			<div class="buttons">
				<button id="appAdmin" class="buttonLeft buttonRight">Administration</button>
			</div>
			<h2>Application</h2>
			<select id="appSelect">
				<!-- Applications are added here as options the designer loads -->
			</select>				
			<br/>
			<h2>Page</h2>
			<select id="pageSelect">
				<!-- Pages are added here as options the designer loads -->
			</select>
			<div class="buttons">
				<button id="pageNew" class="buttonLeft">new page</button>
				<button id="pageEdit" class="buttonRight">edit page</button>
			</div>	
			<div class="buttons">
				<button id="undo" class="buttonLeft" disabled="disabled">undo</button>
				<button id="redo" class="buttonRight" disabled="disabled">redo</button>
			</div>			
			<div class="buttons">
				<button id="pageSave" class="buttonLeft buttonRight">save page</button>
			</div>
			<div class="buttons">
				<button id="pageView" class="buttonLeft buttonRight">view page</button>
			</div>		
			<div id="controlControls">
				<h2>Controls</h2>
				<ul class="design-controls" >
					<!-- Controls are added here as list items when the designer loads -->
				</ul>					
			</div>	
			<div class="controlPanelVersion" >
				<img src="images/RapidLogo_60x40.png" style="margin-left:-16px;"/>
				<div id="controlPanelVersion">Rapid<br/>2.0.1</div>
			</div>									
		</div>
		
		<div id="propertiesPanel" style="z-index:10005">
			<div class="untilsPanelDiv">
			
				<div class="buttons">
					<button id="selectPeerLeft" class="buttonLeft"><img src="images/moveLeft_16x16.png" title="Select the control before this one"/></button>
					<button id="selectParent"><img src="images/moveUp_16x16.png" title="Select the parent of this control"/></button>
					<button id="selectChild"><img src="images/moveDown_16x16.png" title="Select the first child of this control"/></button>
					<button id="selectPeerRight" class="buttonRight"><img src="images/moveRight_16x16.png" title="Select the control after this one"/></button>
				</div>													
				<div class="buttons">
					<button id="swapPeerLeft" class="buttonLeft"><img src="images/swapLeft_16x16.png" title="Move a position backwards"/></button>
					<button id="addPeerLeft"><img src="images/addLeft_16x16.png" title="Add a new control before this one"/></button>
					<button id="deleteControl">&nbsp;<img src="images/bin_16x16.png" title="Delete this control"/>&nbsp;</button>
					<button id="addPeerRight"><img src="images/addRight_16x16.png" title="Add a new control after this one"/></button>
					<button id="swapPeerRight" class="buttonRight"><img src="images/swapRight_16x16.png" title="Move a position forwards"/></button>
				</div>						
				<div class="buttons">
					<button id="copy" class="buttonLeft">&nbsp;copy</button>
					<button id="paste" class="buttonRight">paste</button>
				</div>									
			</div>			
			<div class="propertiesPanelDiv">
				<h2>Properties</h2>
			</div>			
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
<center><h3>You do not have permission to access the Rapid Designer - contact your administrator</h3></center>
<%		
	}
%>			
</body>
</html>