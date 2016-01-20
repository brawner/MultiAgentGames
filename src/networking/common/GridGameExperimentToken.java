package networking.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GridGameExperimentToken extends Token {
	public static final String MATCHES = "matches";
	public static final String MAX_TURNS = "max_turns";
	public static final String MAX_ROUNDS = "max_rounds";

	private static final long serialVersionUID = -2961848723811322928L;

	public GridGameExperimentToken() {
		// TODO Auto-generated constructor stub
	}
	
	public GridGameExperimentToken(Map<String, Object> map) {
		this.putAll(map);
	}
	

	public static GridGameExperimentToken tokenFromJSONString(String jsonStr) {
		Map<String, Object> map = Token.mapFromJSONString(jsonStr);
		return new GridGameExperimentToken(map);
	}

	@Override
	public GridGameExperimentToken getToken(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : new GridGameExperimentToken((LinkedHashMap<String, Object>)object);
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	public List<GridGameExperimentToken> getTokenList(String key) throws TokenCastException {
		List<Object> objects = this.getList(key);
		if (objects == null) {
			return null;
		}
		
		List<GridGameExperimentToken> list = new ArrayList<GridGameExperimentToken>(objects.size());
		try {
			for (Object object : objects) {
				list.add(new GridGameExperimentToken((LinkedHashMap<String, Object>)object));
			}
			return list;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}

}
