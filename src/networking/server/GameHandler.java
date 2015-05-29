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

/**
 * Handles direct game management with the connected client.
 * @author brawner
 *
 */
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
	
	/**
	 * The websocket session for this connection.
	 */
	private Session session;
	
	/**
	 * The associated network agent associated with this connection. Currently, it only handles a single agent for each connection.
	 */
	private NetworkAgent agent;
	
	/**
	 * The thread id this game handler is running with.
	 */
	private String threadId;
	
	/**
	 * The connected users current score. This is probably not where this should be tracked.
	 */
	private double currentScore;
	private SGDomain domain;
	
	public GameHandler(GridGameManager server, Session session, String threadId) {
		this.session = session;
		this.threadId = threadId;
		this.currentScore = 0.0;
	}
	
	/**
	 * When a message is received by the server, it is also processed here. Pretty much only handles action updates, that are sent to the
	 * associated NetworkAgent.
	 * @param msg
	 * @param response
	 */
	public void onMessage(GridGameServerToken msg, GridGameServerToken response) {
		try {
			String msgType = msg.getString(GridGameManager.MSG_TYPE);
			if (msgType.equals(JOIN_GAME)) {
				
				
				response.setString(GridGameManager.MSG_TYPE, INITIALIZE);
				response.setString(GridGameManager.WORLD_ID, threadId);
				
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

	/** 
	 * When the NetworkAgent needs a new action, it can ask the client for a new action.
	 * @param s
	 */
	public void begForAction(State s) {
		GridGameServerToken request = new GridGameServerToken();
		request.setString(GridGameManager.MSG_TYPE, ACTION_REQUEST);
		request.setState(STATE, s, this.domain);
		this.session.getRemote().sendStringByFuture(request.toJSONString());
	}
	
	/**
	 * When any update happens to the state, the client needs to be notified.
	 * @param state
	 */
	public void updateClient(State state) {
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, state, this.domain);
		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(GridGameManager.MSG_TYPE, UPDATE);
		this.session.getRemote().sendStringByFuture(msg.toJSONString());
	}

	/**
	 * After each joint action, the world needs to update the client. This handles that update.
	 * @param s
	 * @param jointAction
	 * @param jointReward
	 * @param sprime
	 * @param isTerminal
	 */
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
		msg.setString(GridGameManager.MSG_TYPE, UPDATE);
		this.session.getRemote().sendStringByFuture(msg.toJSONString());
	}
	
	/**
	 * If any other message needs to be sent to the client, it is send here.
	 * @param message
	 */
	public void updateClient(GridGameServerToken message) {
		this.session.getRemote().sendStringByFuture(message.toJSONString());
	}
	
	/**
	 * Once a game has been completed, it's updated
	 */
	public void gameCompleted() {
		GridGameServerToken msg = new GridGameServerToken();
		msg.setString(GridGameManager.MSG_TYPE, GAME_COMPLETED);
		msg.setDouble(SCORE, this.currentScore);
		this.updateClient(msg);
	}
	
	/**
	 * When the game is finished, the handler notifies the client.
	 */
	public void shutdown() {
		this.gameCompleted();
	}
	
	/**
	 * Adds a new NetworkAgent to the world.
	 * @param world
	 */
	public void addNetworkAgent(World world) {
		this.agent = new NetworkAgent(this);
		this.domain = world.getDomain();
		
		AgentType agentType = 
				new AgentType(GridGame.CLASSAGENT, this.domain.getObjectClass(GridGame.CLASSAGENT), this.domain.getSingleActions());
		
		agent.joinWorld(world, agentType);
	}

	public boolean isConnected() {
		return this.session.isOpen();
	}
	
	

}
