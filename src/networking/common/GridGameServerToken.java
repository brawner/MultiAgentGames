package networking.common;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import networking.server.GameHandler;
import burlap.mdp.core.Domain;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.oo.ObjectParameterizedAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;


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
	
	public State getState(String key) {
		Object object = this.getObject(key);
		if (object == null) {
			return null;
		}
		try {
			State state = (State)object;
			return state;
		
		} catch (ClassCastException e) {
			throw e;
		}
	}
	
	public void setState(String key, State state) {
		this.setObject(key, state);
	}
	
	public static GridGameServerToken tokenFromJointAction(JointAction jointAction) {
		GridGameServerToken token = new GridGameServerToken();
		List<Object> actionTokens = new ArrayList<Object>();
		List<Action> actions = jointAction.getActions();
		for (int agentNum = 0; agentNum < actions.size(); agentNum++) {
			Action action = actions.get(agentNum);
			GridGameServerToken actionToken = new GridGameServerToken();
			actionToken.setString(GameHandler.ACTION, action.actionName());
			actionToken.setInt(GameHandler.AGENT, agentNum);
			if (action instanceof ObjectParameterizedAction) {
				String[] params = ((ObjectParameterizedAction)action).getObjectParameters();
				actionToken.setStringList(GameHandler.ACTION_PARAMS, Arrays.asList(params));
			}
			actionTokens.add(actionToken);
		}
		
		token.setList(JOINT_ACTION, actionTokens);
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
				Action singleAction = new SimpleAction(actionName);
				jointAction.addAction(singleAction);
			}
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
		return jointAction;
		
	}
}
