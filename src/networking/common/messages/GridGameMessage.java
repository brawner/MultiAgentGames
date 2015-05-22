package networking.common.messages;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GridGameMessage extends LinkedHashMap<String, Object>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -645184658826620463L;

	public GridGameMessage() {
	}
	
	public String toJSONString() {
		return null;
	}
	
	protected static GridGameMessage fromJSONString(String json) {
		return null;
	}
	
	public String toJavascriptObject() {
		Map<String, String> fields = this.messageFields();
		Map<String, String> javaScriptFields = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			String field = entry.getKey();
			String javaClassType = entry.getValue();
			String jsEquiv = this.getJavascriptClass(javaClassType);
			javaScriptFields.put(field, jsEquiv);
		}
		
		return this.writeJavaScriptFunction(this.getClass().getSimpleName(), fields.keySet(), javaScriptFields);
		
	}
	
	private String writeJavaScriptFunction(String name, Collection<String> fields, Map<String, String> fieldLookups) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("var ").append(name).append(" = function(");
		Iterator<String> it = fields.iterator();
		while (it.hasNext()) {
			String next = it.next();
			next = this.getConstructorName(next);
			next += (it.hasNext()) ? ", " : "";
			buffer.append(next);
		}
		buffer.append(") {\n");
		
		for (Map.Entry<String, String> entry : fieldLookups.entrySet()) {
			buffer = this.writeJavaScriptField(buffer, entry.getKey(), this.getConstructorName(entry.getKey()), entry.getValue());
		}
		
		for (Map.Entry<String, String> entry : fieldLookups.entrySet()) {
			buffer = this.writeJavaScriptGetterSetter(buffer, entry.getKey(), entry.getValue());
		}
		
		
		buffer.append("};");
		
		return buffer.toString();
	}
	
	private StringBuffer writeJavaScriptField(StringBuffer buffer, String fieldName, String constructorName, String classType) {
		
		buffer = this.writeFieldCheck(buffer, constructorName, classType);
		buffer.append("var ").append(fieldName).append(" = ").append(constructorName).append("\n");
		return buffer;
	}
	
	private StringBuffer writeJavaScriptGetterSetter(StringBuffer buffer, String fieldName, String classType) {
		
		buffer.append("this.").append(this.getGetterName(fieldName)).append(" = function() {\n");
		buffer.append("return ").append(fieldName).append(";\n}\n");
		
		buffer.append("this.").append(this.getSetterName(fieldName)).append(" = function(").append(this.getConstructorName(fieldName)).append(") {\n");
		buffer = this.writeFieldCheck(buffer, this.getConstructorName(fieldName), classType);
		
		buffer.append(fieldName).append(" = ").append(this.getConstructorName(fieldName)).append(";\n}\n");
		
		return buffer;
	}
	
	private String getConstructorName(String fieldName) {
		return "_" + fieldName;
	}
	
	private String getGetterName(String fieldName) {
		return "get" + fieldName;
	}
	
	private StringBuffer writeFieldCheck(StringBuffer buffer, String varName, String classType) {
		buffer.append("if (!(").append(varName).append(" instanceof ").append(classType).append(")) {\n");
		buffer.append("throw \"").append(varName).append(" is not of the class type ").append(classType).append("\";\n}");
		return buffer;
	}
	
	private String getSetterName(String fieldName) {
		return "set" + fieldName;
	}
	
	public abstract Map<String, String> messageFields();
	
	protected String getJavascriptClass(String javaClass) {
		switch(javaClass) {
		case "Integer":
			return "Number";
		case "Boolean":
			return "Boolean";
		case "String":
			return "String";
		case "Double":
			return "Number";
		default:
			return null;
		
		}
	}
}
