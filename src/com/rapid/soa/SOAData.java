package com.rapid.soa;

public class SOAData {
				
	SOASchema _soaSchema;
	SOAElement _rootElement;
	static int _column;
	
	public SOAData(SOAElement rootElement) {
		_rootElement = rootElement;	
	}
	
	public SOAData(SOASchema soaSchema) {
		_soaSchema = soaSchema;
	}
	
	public SOAData(SOAElement rootElement, SOASchema soaSchema) {
		_rootElement = rootElement;
		_soaSchema = soaSchema;		
	}
	
	public SOAElement getRootElement() { return _rootElement; }
	public void setRootNode(SOAElement rootElement) { _rootElement = rootElement; }
					
	@Override
	public String toString() {
		_column = 0;
		if (_rootElement == null) {
			return null;
		} else {
			return _rootElement.toString();	
		}		
	}

}
