package com.rapid.core;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.logging.log4j.LogManager;

import com.rapid.server.RapidHttpServlet;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;

@XmlRootElement
public class Email {
	
	// public static class
	public static class RapidMailAuthenticator extends javax.mail.Authenticator {
    	
    	private String _userName, _password;
    	
    	public RapidMailAuthenticator(String userName, String password) {
    		_userName = userName;
    		_password = password;
    	}
    	
    	protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(_userName, _password);
        }
    }
	
	// private static variables
	private static Properties _properties;
	private static Authenticator _authenticator;
	private static Email _email;
	
	// private instance variables
	private String _host, _security, _userName, _password;
	private int _port;
	
	// properties
	public String getHost() { return _host; }
	public void setHost(String host) { _host = host; }
	
	public int getPort() { return _port; }
	public void setPort(int port) { _port = port; }
	
	public String getSecurity() { return _security; }
	public void setSecurity(String security) { _security = security; }
	
	public String getUserName() { return _userName; }
	public void setUserName(String userName) { _userName = userName; }
	
	@XmlJavaTypeAdapter( EncryptedXmlAdapter.class )
	public String getPassword() { return _password; }
	public void setPassword(String password) { _password = password; }
	
	// constructors
	public Email() {}
	public Email(String host, int port, String security, String userName, String password) {
		_host = host;
		_port = port;
		_security = security;
		_userName = userName;
		_password = password;
	}
	
	// instance methods
	public void save(ServletContext servletContext) throws JAXBException, IOException {
		
		// get the file in which the control xml files are stored
		File file = new File(servletContext.getRealPath("/WEB-INF/email/email.xml"));
		// make dirs if need be
		if (!file.exists()) file.getParentFile().mkdirs();
		
		// get the marshaller from the context
		Marshaller marshaller = RapidHttpServlet.getMarshaller();
		// marshall the devices to the file
		marshaller.marshal(this, file);
		
		// retain this object in our static
        _email = this;
        
	}
	
	// static methods
    public static Email load(ServletContext servletContext) throws JAXBException, IOException {
       
        // placeholder for the object
        Email email = null;
       
        // get the file in which the email xml file is stored
        File file = new File(servletContext.getRealPath("/WEB-INF/email/email.xml"));
       
        // if it exists
        if (file.exists()) {
            try {
                // get the unmarshaller from the context
                Unmarshaller unmarshaller = RapidHttpServlet.getUnmarshaller();   
                // unmarshall the devices
                email = (Email) unmarshaller.unmarshal(file);
            } catch (Exception ex) {
                // log
                LogManager.getLogger(Email.class).error("Error loading email settings", ex);
            }
        }

        // if we got one
        if (email != null) {
                  
            // add email to the servlet context
            servletContext.setAttribute("email", email);

            // set the properties we've just loaded
            setProperties(email.getHost(), email.getPort(), email.getSecurity(), email.getUserName(), email.getPassword());
                        
        }
        
        // retain this object in our static
        _email = email;
           
        // return the email object   
        return email;
       
    }
    
    public static Email getEmailSettings() {
    	return _email;
    }
    
    public static void setProperties(String host, int port, String security, String userName, String password) {

	    // Get system properties
	    Properties props = new Properties();

	    // Setup mail server
	    props.put("mail.smtp.host", host);
	    props.put("mail.smtp.port", port);
	    
	    // check security - this article was very useful at describing the difference: https://luxsci.com/blog/ssl-versus-tls-whats-the-difference.html
	    if ("ssl".equals(security)) {
	    		    	
	        props.put("mail.smtp.auth", "true");
	        props.put("mail.smtp.ssl.enable", "true");
	    	
	    } else if ("tls".equals(security)) {
	    
	    	props.put("mail.smtp.auth", "true");
		    props.put("mail.smtp.starttls.enable", "true"); 
		    props.put("mail.smtp.socketFactory.fallback", "true");
	    	
	    } 
	    
	    // retain these in a static
	    _properties = props;
	    
	    // update the static object with these new values
    	_email = new Email(host, port, security, userName, password);
	    
	    // empty the authenticator so a new one is made on the first send
	    _authenticator = null;

	}
    
    public static void send(String from, String to, String subject, String text) throws MessagingException {
    	
	    // if _authenticator is null
	    if (_authenticator == null) {
	    	// make a new one and save in static
	    	_authenticator = new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(_email.getUserName(), _email.getPassword());
	            }
	        };
	    }
	    
	    // get the default session
    	Session session = Session.getInstance(_properties, _authenticator);
    	
    	// create the message
    	Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(text);

        // send it!
        Transport.send(message);
    	
    }
    
    // 
    
	
}
