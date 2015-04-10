package com.rapid.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class RapidSessionListener implements HttpSessionListener {
	
    final private List<HttpSession> _sessions;
    
    public RapidSessionListener() {
    	_sessions = new ArrayList<HttpSession>() ; 
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {   	
    	List<HttpSession> sessions = Collections.synchronizedList(_sessions);
    	HttpSession session = event.getSession();
    	sessions.add(session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
    	List<HttpSession> sessions = Collections.synchronizedList(_sessions);
    	HttpSession session = event.getSession();
    	sessions.remove(session);
    }

    public int getTotalSessionCount() {
    	List<HttpSession> sessions = Collections.synchronizedList(_sessions);
    	return sessions.size();
    }
    
}