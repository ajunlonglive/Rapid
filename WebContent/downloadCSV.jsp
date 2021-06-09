<%@ page language="java" contentType="text/csv; charset=UTF-8" pageEncoding="UTF-8"%><%@ page import="org.json.JSONObject" %><%@ page import="org.json.JSONArray" %><%!

/*

How to use this facility in Rapid

1) Use the downloadCSV action, or

2.1) Use a panel or other control to add the following HTML into a Rapid page (the filename input is optional):
	
	<form id="downloadForm" action="downloadCSV.jsp" method="post" target="_blank" style="display:none;">
	<input id="downloadFileName" name="downloadFileName" value="filename.csv"/>
	<input id="downloadData" name="downloadData"/>
	</form>
	
2.2) Use a custom action to populate the form inputs and submit the form:
	
	// create an object for the data with rows and headers
	var data = {rows:[],headers:[]};
	// an array for the column headers
	// loop the rows
	$("#P1_C51_").find("tr").each( function(idx) {
	  // add a rows collection if not header row
	  if (idx > 0) data.rows.push([]);
	  // loop the cells
	  $(this).find("td").each( function() {
	    // get a ref to the cell
	    var c = $(this);
	    // ignore hidden columns
	    if (c.is(":visible")) {
	      // if this the header row
	      if (idx == 0) {
	        // add to headers
	        data.headers.push(c.text());
	      } else {
	        data.rows[idx-1].push(c.text());
	      }
	    }
	  });
	});
	// stringify it so it can be sent in the form
	data =  JSON.stringify(data);	
	// put it in the form input
	$("#downloadData").val(data);
	// submit the form
	$("#downloadForm").submit();

*/

public String escapeValue(String value) {
	if (value == null) {
		return null;
	} else {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}

%><%
	// Expect that the data from the front-end is encoded as UTF-8
	request.setCharacterEncoding("UTF-8");

	// get any file name provided to us
	String fileName = request.getParameter("downloadFileName");

	// if we didn't get a filename set to default
	if (fileName == null) fileName = "download.csv";

	// set response filename for automatic download
	response.setHeader("Content-Disposition","attachment; filename=\"" + fileName + "\"");
	
	// get any downloadData
	String data = request.getParameter("downloadData");
	
	// if not data yet look for backwards compatible location
	if (data == null) data = request.getParameter("data");
	
	// if we got some data
	if (data != null) {
		
		// turn it into a proper json object 
		JSONObject jsonData = new JSONObject(data);
		
		// get its rows 2d-array
		JSONArray rows = jsonData.getJSONArray("rows");
				
		// look for a headers collection - this was added by looping the grid header row columns 
		JSONArray headers = jsonData.optJSONArray("headers");
		
		// if we got any headers 
		if (headers != null) {
			// this is the dynamic header list, so loop it
			for (int i = 0; i < headers.length(); i++) {
				// print the escaped header
				String header = escapeValue(headers.optString(i));
				out.write(header.replaceAll("[^ -~]", ""));
				
				// print the comma if not the last value
				if (i < headers.length() - 1) out.write(",");
			}
			// print the line break
			out.write("\r\n");
		}

		// now loop the rows
		for (int i = 0; i < rows.length(); i++) {
			// get this row
			JSONArray row = rows.getJSONArray(i);
			// loop the cells in the row
			for (int j = 0; j < row.length(); j++) {
				// print the escaped col value
				out.write(escapeValue(row.optString(j)));
				// print the comma if not the last value
				if (j < row.length() - 1) out.write(",");
			}				
			// print the line break
			out.write("\r\n");
		}		
	}

%>