/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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