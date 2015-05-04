package networking.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import networking.client.SGVisualExplorerClient;
import networking.common.GridGameServerToken;
import networking.common.TokenCastException;

import org.eclipse.jetty.websocket.api.Session;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;
import burlap.oomdp.stochasticgames.World;


public class GameHandler {
	public final static String MSG_TYPE = "MESSAGE_TYPE";
	public final static String TAKE_ACTION = "take_action";
	public final static String INITIALIZE_GAME = "init_game";
	public final static String ADD_AGENT = "add_agent";
	public final static String JOIN_GAME = "join_game";
	public final static String EXIT_GAME= "exit_game";
	public final static String RUN_GAME = "run_game";
	public final static String LOAD_WORLDS = "load_worlds";
	public final static String REMOVE_GAME = "remove_game";
	public final static String AGENT_TYPE = "agent_type";
	public final static String ACTION_REQUEST = "action_sorely_needed";
	public final static String STATE = "state";
	public final static String ACTION = "action";
	public final static String ACTION_PARAMS = "action_params";
	public final static String AGENT = "agent";
	public final static String REWARD = "reward";
	public final static String IS_TERMINAL = "is_terminal";
	public final static String UPDATE = "update";
	public final static String RESULT = "result";
	public final static String SUCCESS = "success";
	public final static String INITIALIZE = "initialize";
	public final static String HANDLER_RESPONSE = "handler_response";
	public static final String CLOSE_GAME = "close_game";
	
	private Session session;
	private World world;
	private SGDomain domain;
	private NetworkAgent agent;
	private GridGameServer server;
	private String threadId;
	
	public GameHandler(GridGameServer server, Session session, World world, SGDomain domain, String threadId) {
		this.session = session;
		this.world = world;
		this.domain = domain;
		this.server = server;
		this.threadId = threadId;
	}
	
	public GridGameServerToken onMessage(GridGameServerToken msg) {
		GridGameServerToken response = new GridGameServerToken();
		try {
			String msgType = msg.getString(MSG_TYPE);
			if (msgType.equals(JOIN_GAME)) {
				String agentTypeStr = msg.getString(AGENT_TYPE);
				this.agent = new NetworkAgent(this);
				
				AgentType agentType = 
						new AgentType(GridGame.CLASSAGENT, this.domain.getObjectClass(GridGame.CLASSAGENT), this.domain.getSingleActions());
				
				agent.joinWorld(this.world, agentType);
				
				response.setString(MSG_TYPE, INITIALIZE);
				response.setString(AGENT, agent.getAgentName());
				response.setString(GridGameServer.WORLD_TYPE, world.toString());
				response.setString(GridGameServer.WORLD_ID, threadId);
				SGDomain domain = world.getDomain();
				StateJSONParser parser = new StateJSONParser(domain);
				response.setObject(GameHandler.STATE, parser.getJSONPrepared(world.startingState()));
				
				
				response.setString(RESULT, SUCCESS);
				
			} else if (msgType.equals(TAKE_ACTION)) {
				String actionName = msg.getString(ACTION);
				List<String> actionParams = msg.getStringList(ACTION_PARAMS);
				String[] params = actionParams.toArray(new String[actionParams.size()]);
				SingleAction action = this.domain.getSingleAction(actionName);
				GroundedSingleAction groundedAction = new GroundedSingleAction(agent.getAgentName(), action, params);
				this.agent.setNextAction(groundedAction);
				response.setString(RESULT, SUCCESS);
			}
		} catch (TokenCastException e) {
			response.setError(true);
		}
		return response;
	}

	public void begForAction(State s) {
		GridGameServerToken request = new GridGameServerToken();
		request.setString(MSG_TYPE, ACTION_REQUEST);
		request.setState(STATE, s, this.domain);
		
		try {
			if (this.session != null) {
				this.session.getRemote().sendString(request.toJSONString());
			} 
		} catch (IOException e) {
		}
		
		
		
	}

	public void updateClient(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, sprime, this.domain);
		token.setToken(ACTION, GridGameServerToken.tokenFromJointAction(jointAction));
		token.setObject(REWARD, jointReward);
		token.setBoolean(IS_TERMINAL, isTerminal);
		
		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(MSG_TYPE, UPDATE);
		try {
			if (this.session != null) {
				this.session.getRemote().sendString(msg.toJSONString());
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		GridGameServerToken token = new GridGameServerToken();
		token.setString(GameHandler.MSG_TYPE, GameHandler.CLOSE_GAME);
		try {
			if (this.session != null) {
				this.session.getRemote().sendString(token.toJSONString());
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	
	

}
