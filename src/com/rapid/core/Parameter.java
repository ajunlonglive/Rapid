package com.rapid.core;

// this class is referenced by both the database and webservice action classes
public class Parameter {
			
	private String _itemId, _field;
	
	public String getItemId() { return _itemId; }
	public void setItemId(String itemId) { _itemId = itemId; }
	
	public String getField() { return _field; }
	public void setField(String field) { _field = field; }
	
	public Parameter() {};
	public Parameter(String itemId, String field) {
		_itemId = itemId;
		_field = field;
	}
	
}
