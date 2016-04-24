/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.server;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

public class RapidSessionListener implements HttpSessionListener {
	
    private static Map<String, HttpSession> _sessions;
    private Logger _logger;
    
    public RapidSessionListener() {
    	// instantiate the concurrent hash map which should make it thread safe, alternatively there is the Collections.synchronizedMap
    	_sessions = new ConcurrentHashMap<String,HttpSession>() ;    	
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {   	
    	HttpSession session = event.getSession();
    	String sessionId = session.getId(); 
    	_sessions.put(sessionId, session);
    	if (_logger == null) _logger = Logger.getLogger(this.getClass());
    	_logger.debug("Session created " + sessionId);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
    	HttpSession session = event.getSession();
    	_sessions.remove(session.getId());
    	if (_logger == null) _logger = Logger.getLogger(this.getClass());
    	_logger.debug("Session destroyed " + session.getId());
    }
    
    public synchronized static HttpSession  getSession(String sessionId) {
    	if (_sessions == null) {
    		return null;
    	} else {
    		return _sessions.get(sessionId);
    	}
    }
    
    public synchronized static Map<String,HttpSession>  getSessions() {
    	return _sessions;
    }
    
    public synchronized static int getTotalSessionCount() {
    	if (_sessions == null) {
    		return -1;
    	} else {
    		Map<String,HttpSession> sessions = Collections.synchronizedMap(_sessions);
    		return sessions.size();
    	}
    }
    
}