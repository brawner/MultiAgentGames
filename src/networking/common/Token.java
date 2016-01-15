package networking.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Token extends LinkedHashMap<String, Object> {
	private static final String ERROR_KEY = "Error";
	
	public Token() {
		// TODO Auto-generated constructor stub
	}
	
	public abstract Token getToken(String field) throws TokenCastException ;
	public abstract List<? extends Token> getTokenList(String field) throws TokenCastException;
	
	/** Serializer which serializes the token into a message format */
	public String toJSONString() {
		JsonFactory jsonFactory = new JsonFactory();
		StringWriter writer = new StringWriter();
		JsonGenerator jsonGenerator;
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			jsonGenerator = jsonFactory.createGenerator(writer);
			objectMapper.writeValue(jsonGenerator, this);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return writer.toString();
	}
	
	/** Static method constructs a Token from JSON version of a DB object 
	 *
	 * @param jsonStr	String JSON object to be parsed
	 * @return			Token with one or more DBobjs
	 */
	public static Map<String, Object> mapFromJSONString(String jsonStr) {
		JsonFactory jsonFactory = new JsonFactory();
		Map<String, Object> objects = null;
		try {
			ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
			TypeReference<Map<String, Object>> listTypeRef = 
					new TypeReference<Map<String, Object>>() {};
			objects = objectMapper.readValue(jsonStr, listTypeRef);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return objects;
	}
	
	public static GridGameServerToken tokenFromFile(String filename) {
		//System.out.println("Processing " + filename);
		Path path = Paths.get(filename);
		if (!Files.exists(path)) {
			System.err.println(filename + " does not exist");
			return null;
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		StringBuilder builder = new StringBuilder();
		try {
			String line = reader.readLine();
			while (line != null) {
				builder.append(line);
				line = reader.readLine();
			}
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return GridGameServerToken.tokenFromJSONString(builder.toString());
	}
	
	/** Retrieves the value at the provided key
	 * 
	 *  @param key	String key for the desired hashmap entry
	 *  @return		Object stored at the keyed location
	 */
	public Object getObject(String key) {
		return this.get(key);
	}
	
	/** Sets the Object value at a keyed location
	 * 
	 * @param key		String with the key for a location in the map
	 * @param object	Object to be stored at the location
	 */
	public void setObject(String key, Object object) {
		this.put(key, object);
	}
	
	/** Sets the value at ERROR_KEY to the provided value
	 * 
	 * @param isError	Boolean which is true if error or false if not
	 */
	public void setError(Boolean isError) {
		this.put(Token.ERROR_KEY, isError);
	}
	
	/** Retrieves the value stored at key "Error" as a Boolean. 
	 * 
	 * @return	Boolean equal to the value at ERROR_KEY or true if the value isn't castable, otherwise false if ERROR_KEY isn't in the hashmap. 
	 */
	public Boolean getError() {
		if (this.containsKey(Token.ERROR_KEY)) {
			try {
				return this.getBoolean(Token.ERROR_KEY);
			} catch (TokenCastException e) {
				return true;
			}
		}
		return false;
	}
	
	/** Retrieves value at key and casts to String
	 * 
	 * @param key	String with the key for the location
	 * @returns		String casted Object value from keyed location
	 * @throws		TokenCastException iff cast throws an exception
	 */
	public String getString(String key) throws TokenCastException {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : (String)object;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at key with a String value
	 * 
	 * @param key		String key for location
	 * @param value		String value to set
	 */
	public void setString(String key, String value) {
		this.setObject(key, value);
	}
	
	/** Retrieves value at key and casts to Boolean
	 * 
	 * @param key	String with the key for the location
	 * @returns		Boolean casted Object value from keyed location
	 * @throws		TokenCastException iff cast throws an exception
	 */
	public Boolean getBoolean(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : (Boolean)object;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at key with a Boolean value
	 * 
	 * @param key		String key for location
	 * @param value		Boolean value to set
	 */
	public void setBoolean(String key, Boolean value) {
		this.setObject(key, value);
	}
	
	/** Retrieves value at key and casts to Double
	 * 
	 * @param key	String with the key for the location
	 * @returns		Double casted Object value from keyed location
	 * @throws		TokenCastException iff cast throws an exception
	 */
	public Double getDouble(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : (Double)object;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at key with a Double value
	 * 
	 * @param key		String key for location
	 * @param value		Double value to set
	 */
	public void setDouble(String key, Double value) {
		this.setObject(key, value);
	}
	
	/** Retrieves value at key and casts to Integer
	 * 
	 * @param key	String with the key for the location
	 * @returns		Integer casted Object value from keyed location
	 * @throws		TokenCastException iff cast throws an exception
	 */
	public Integer getInt(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : (Integer)object;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at key with a Integer value
	 * 
	 * @param key		String key for location
	 * @param value		Integer value to set
	 */
	public void setInt(String key, Integer value) {
		this.setObject(key, value);
	}
	
	/** Retrieves value at key and casts to a List of Objects
	 * 
	 * @param key	String with the key for the location
	 * @returns		List<Object> casted Object value from keyed location
	 * @throws		TokenCastException iff cast throws an exception
	 */
	public List<Object> getList(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : (List<Object>)object;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at key with a List<Object> value
	 * 
	 * @param key		String key for location
	 * @param value		List<Object> value to set
	 */
	public void setList(String key, List<Object> list) {
		this.setObject(key, list);
	}
	
	/** Retrieves the list of values at the key and composes a List of Strings from it
	 * 
	 * @param key	String with key for the location to retrieve
	 * @return		List<String> composed from DBList at key
	 * @throws		TokenCastException iff cast throws and exception
	 */
	public List<String> getStringList(String key)  throws TokenCastException  {
		List<Object> objects = this.getList(key);
		if (objects == null) {
			return null;
		}
		
		List<String> list = new ArrayList<String>(objects.size());
		try {
			for (Object obj : objects) {
				list.add((String)obj);
			}
			return list;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	/** Sets the value at the keyed location to a List of Strings
	 * 
	 * @param key	String with key for location to set
	 * @param list	List<String> to set as the value at the keyed location 
	 */
	public void setStringList(String key, List<String> list) {
		this.setObject(key, list);
	}

	/** Sets the value at key with a String value casted to object
	 * @param key		String key for the location to store the subtoken
	 * @param value		RobocookServerToken to store as a subtoken at the location
	 */
	public void setToken(String key, Token value) {
		this.setObject(key, value);
	}
	
	public void setTokenList(String key, List<? extends Token> list) {
		this.setObject(key, list);
	}
}
