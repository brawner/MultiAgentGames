package networking.common;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import networking.server.GameHandler;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.states.State;
import burlap.oomdp.legacy.StateJSONParser;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.agentactions.GroundedSGAgentAction;
import burlap.oomdp.stochasticgames.agentactions.ObParamSGAgentAction.GroundedObParamSGAgentAction;
import burlap.oomdp.stochasticgames.agentactions.SGAgentAction;


/**
 * Extends a linked hash map with additional, server specific methods
 * 
 * @author Stephen Brawner
 * @author Lee Painton
 */
public class GridGameServerToken extends Token {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4093923226101335322L;
	/** String key at which error status is to be stored */
	public static final String JOINT_ACTION = "joint_action";
	/** RobocookServerToken default constructor */
	public GridGameServerToken() {
		
	}
		
	/** Constructor which imports a linked hash map into the structure of the Token 
	 * 
	 * @param map 	LinkedHashMap<String, Object> which is imported into the structure of the token.
	 */
	public GridGameServerToken(Map<String, Object> map) {
		this.putAll(map);
	}
	

	public static GridGameServerToken tokenFromJSONString(String jsonStr) {
		Map<String, Object> map = Token.mapFromJSONString(jsonStr);
		return new GridGameServerToken(map);
	}
	
	/** Retrieves individual token if this token is composed of multiple DBobjs
	 * 
	 * @param key	Key under which the subtoken is stored
	 * @return		RobocookServerToken composed from the subtoken
	 * @throws		TokenCastException if the value isn't castable to a LinkedHashMap<String, Object>
	 */
	@Override
	public GridGameServerToken getToken(String key)  throws TokenCastException  {
		try {
			Object object  = this.getObject(key);
			return (object == null) ? null : new GridGameServerToken((LinkedHashMap<String, Object>)object);
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
		
	public List<GridGameServerToken> getTokenList(String key) throws TokenCastException {
		List<Object> objects = this.getList(key);
		if (objects == null) {
			return null;
		}
		
		List<GridGameServerToken> list = new ArrayList<GridGameServerToken>(objects.size());
		try {
			for (Object object : objects) {
				list.add(new GridGameServerToken((LinkedHashMap<String, Object>)object));
			}
			return list;
		}
		catch (ClassCastException e) {
			throw e;
		}
	}
	
	public State getState(String key, Domain domain) {
		Object object = this.getObject(key);
		if (object == null) {
			return null;
		}
		try {
			List<Map<String, Object>> objects = (List<Map<String, Object>>)object;
			StateJSONParser parser = new StateJSONParser(domain);
			return parser.JSONPreparedToState(objects);
		
		} catch (ClassCastException e) {
			throw e;
		}
	}
	
	public void setState(String key, State state, Domain domain) {
		StateJSONParser parser = new StateJSONParser(domain);
		List<Map<String, Object>> objects = parser.getJSONPrepared(state);
		this.setObject(key, objects);
	}
	
	public static GridGameServerToken tokenFromJointAction(JointAction jointAction) {
		GridGameServerToken token = new GridGameServerToken();
		List<Object> actions = new ArrayList<Object>();
		for (Map.Entry<String, GroundedSGAgentAction> entry : jointAction.actions.entrySet()) {
			GroundedSGAgentAction action = entry.getValue();
			GridGameServerToken actionToken = new GridGameServerToken();
			actionToken.setString(GameHandler.ACTION, action.actionName());
			String agentName = entry.getKey();
			actionToken.setString(GameHandler.AGENT, agentName);
			actionToken.setStringList(GameHandler.ACTION_PARAMS, Arrays.asList(action.getParametersAsString()));
			
			actions.add(actionToken);
		}
		
		token.setList(JOINT_ACTION, actions);
		return token;
	}

	public static JointAction jointActionFromToken(
			GridGameServerToken actionToken, SGDomain domain) {
		if (actionToken == null) {
			return null;
		}
		JointAction jointAction = new JointAction();
		try {
			List<Object> actions = actionToken.getList(JOINT_ACTION);
			if (actions == null) {
				return null;
			}
			for (Object object : actions) {
				GridGameServerToken action = new GridGameServerToken((LinkedHashMap<String, Object>)object);
				String actionName = action.getString(GameHandler.ACTION);
				String agent = action.getString(GameHandler.AGENT);
				List<String> params = action.getStringList(GameHandler.ACTION_PARAMS);
				SGAgentAction singleAction = domain.getSingleAction(actionName);
				GroundedSGAgentAction groundedAction = new GroundedObParamSGAgentAction(agent, singleAction, params.toArray(new String[params.size()]));
				jointAction.addAction(groundedAction);
			}
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
		return jointAction;
		
	}
}
