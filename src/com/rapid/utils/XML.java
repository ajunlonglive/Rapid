package com.rapid.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XML {
	
	public static Document openDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse (file);
        
        return document;
		
	}
	
	public static void saveDocument(Document document, File file) throws TransformerFactoryConfigurationError, TransformerException {
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Source input = new DOMSource(document);
		Result output = new StreamResult(file);
		
		transformer.transform(input, output);
		
	}
	
	public static Node getChildElement(Node node, String elementName) {
		
		NodeList nodeList = node.getChildNodes();
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			
			Node childNode = nodeList.item(i);
			
			if (elementName.equals(childNode.getNodeName())) return childNode;
			
		}
		
		return null;
		
	}

	public static String getChildElementValue(Node node, String elementName) {
		
		Node childNode = getChildElement(node, elementName);
			
		if (childNode != null) return childNode.getTextContent();
							
		return null;
		
	}
	
	public static String getElementValue(String xml, String elementName) {
		
		String value = null;
		
		int startPos = xml.indexOf("<" + elementName + ">");
	    
	    if (startPos > 0) {
	    	
	    	int endPos = xml.indexOf("</" + elementName + ">");
	    
	    	value = xml.substring(startPos + elementName.length() + 2, endPos);
	    	
	    }
		
		return value;
		
	}
	
	public static String escape(String value) {
		
		return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;").replace("'", "&apos;");
		
	}
	
	public static String unescape(String value) {
		
		return value.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&apos;","'");
		
	}

}
