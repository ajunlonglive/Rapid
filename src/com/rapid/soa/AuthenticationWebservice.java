package com.rapid.soa;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.server.RapidRequest;
import com.rapid.server.RapidSessionListener;
import com.rapid.server.filter.RapidFilter;
import com.rapid.soa.SOASchema.SOASchemaElement;

public class AuthenticationWebservice extends Webservice {
			
	public AuthenticationWebservice() {
		
		// this is the authentication webservice
		_isAuthenticate = true;
		
		// create the request schema
		SOASchema requestSchema = new SOASchema();
		requestSchema.setRootElement(new SOASchemaElement("0","authenticate"));
		requestSchema.addChildElement("username", SOASchema.STRING);
		requestSchema.addChildElement("password", SOASchema.STRING);
		setRequestSchema(requestSchema);
		
		// create the response schema
		SOASchema responseSchema = new SOASchema();
		responseSchema.setRootElement(new SOASchemaElement("0","authentication", SOASchema.STRING));
		setResponseSchema(responseSchema);
		
	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest,	Application application, SOAData requestData)	throws WebserviceException {
		
		List<SOAElement> requestElements = requestData.getRootElement().getChildElements();
		String username = null;
		String password = null;
		for (SOAElement element : requestElements) {
			if ("username".equals(element.getName())) username = element.getValue();
			if ("password".equals(element.getName())) password = element.getValue();
		}
		
		// remember whether we are authorised for at least one application
		boolean authorised = false;
		
		// get the applications collection
		Applications applications = (Applications) rapidRequest.getRapidServlet().getApplications();
		
		// if there are some applications
		if (applications != null) {
			// loop them
			for (Application app : applications.get()) {
				try {
					// see if the user is known to this application
					authorised = app.getSecurityAdapter().checkUserPassword(rapidRequest, username, password);
					// we can exit if so as we only need one
					if (authorised) break;								
				} catch (Exception ex) {
					// log the error
					rapidRequest.getRapidServlet().getLogger().error("Authentication webservice error checking user", ex);
				}
			}
		}
		
		// throw an error if not authorised
		if (!authorised) throw new WebserviceException("Not authorised");
		
		// assume there is no existing session for this user
		HttpSession session = null;
		// get the current sessions
		Map<String, HttpSession> sessions = RapidSessionListener.getSessions();
		// loop the keys
		for (String key : sessions.keySet()) {
			// get this session
			HttpSession s = sessions.get(key);
			// get the user
			String u = (String) s.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
			// check user name
			if (u != null) {
				if (u.equals(username)) {
					// get the password
					String p = (String) s.getAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD);
					// check the password
					if (p != null) {
						if (p.equals(password)) {
							session = s;
							break;
						}
					}
				}
			}
		}
		
		// if we couldn't find an existing session for this user
		if (session == null) {
			// get a new session
			session = rapidRequest.getRequest().getSession();
			// set the user name
			session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME, username);
			// set the password
			session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, password);
		}
				
		// create the response element
		SOAElement responseElement = new SOAElement("authentication",session.getId());
		// create the response data
		SOAData responseData= new SOAData(responseElement);
		// return the response
		return responseData;
	}

}
