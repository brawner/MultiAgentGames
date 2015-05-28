package networking.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import networking.common.GridGameServerToken;
import networking.common.TokenCastException;

import org.eclipse.jetty.websocket.api.Session;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;
import burlap.oomdp.stochasticgames.World;


public class GameHandler {
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
	public static final String CLOSE_GAME = "close_game";
	public static final String SCORE = "score";
	public static final String GAME_COMPLETED = "game_complete";
	public static final String CONFIG_GAME = "config_game";
	
	private Session session;
	private NetworkAgent agent;
	private String threadId;
	private double currentScore;
	private SGDomain domain;
	
	public GameHandler(GridGameServer server, Session session, String threadId) {
		this.session = session;
		this.threadId = threadId;
		this.currentScore = 0.0;
	}
	
	public void onMessage(GridGameServerToken msg, GridGameServerToken response) {
		try {
			String msgType = msg.getString(GridGameServer.MSG_TYPE);
			if (msgType.equals(JOIN_GAME)) {
				
				
				response.setString(GridGameServer.MSG_TYPE, INITIALIZE);
				response.setString(GridGameServer.WORLD_ID, threadId);
				
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
	}

	public void begForAction(State s) {
		GridGameServerToken request = new GridGameServerToken();
		request.setString(GridGameServer.MSG_TYPE, ACTION_REQUEST);
		request.setState(STATE, s, this.domain);
		this.session.getRemote().sendStringByFuture(request.toJSONString());
	}
	
	public void updateClient(State state) {
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, state, this.domain);
		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(GridGameServer.MSG_TYPE, UPDATE);
		this.session.getRemote().sendStringByFuture(msg.toJSONString());
	}

	public void updateClient(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		this.currentScore += jointReward.get(this.agent.getAgentName());
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, sprime, this.domain);
		token.setDouble(SCORE, this.currentScore);
		token.setToken(ACTION, GridGameServerToken.tokenFromJointAction(jointAction));
		token.setObject(REWARD, jointReward);
		token.setBoolean(IS_TERMINAL, isTerminal);
		
		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(GridGameServer.MSG_TYPE, UPDATE);
		this.session.getRemote().sendStringByFuture(msg.toJSONString());
	}
	
	public void updateClient(GridGameServerToken message) {
		this.session.getRemote().sendStringByFuture(message.toJSONString());
	}
	
	public void gameCompleted() {
		GridGameServerToken msg = new GridGameServerToken();
		msg.setString(GridGameServer.MSG_TYPE, GAME_COMPLETED);
		msg.setDouble(SCORE, this.currentScore);
		this.updateClient(msg);
	}
	
	public void shutdown() {
		this.gameCompleted();
	}
	
	public void addNetworkAgent(World world) {
		this.agent = new NetworkAgent(this);
		this.domain = world.getDomain();
		
		AgentType agentType = 
				new AgentType(GridGame.CLASSAGENT, this.domain.getObjectClass(GridGame.CLASSAGENT), this.domain.getSingleActions());
		
		agent.joinWorld(world, agentType);
	}
	
	

}
