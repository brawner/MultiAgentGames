package networking.server;

import java.util.List;
import java.util.Map;

import networking.common.GridGameServerToken;
import networking.common.TokenCastException;

import org.eclipse.jetty.websocket.api.Session;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;

/**
 * Handles direct game management with the connected client.
 * @author brawner
 *
 */
public class GameHandler {
	public final static String TAKE_ACTION = "take_action";
	public final static String ADD_AGENT = "add_agent";
	public final static String JOIN_GAME = "join_game";
	public final static String EXIT_GAME= "exit_game";
	public final static String RUN_GAME = "run_game";
	public final static String LOAD_WORLDS = "load_worlds";
	public final static String REMOVE_GAME = "remove_game";
	public final static String AGENT_TYPE = "agent_type";
	public final static String HEARTBEAT = "heartbeat";
	public final static String ACTION_REQUEST = "action_sorely_needed";
	public final static String STATE = "state";
	public final static String ACTION = "action";
	public final static String ACTIONS = "actions";
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
	public static final String LOG_REACTION_TIME = "log_reation_time";
	public static final String EXPERIMENT_COMPLETED = "experiment_complete";
	public static final String CONFIG_GAME = "config_game";
	public static final String RUN_URL_GAME = "run_url_game";
	public static final String AGENT_NAME = "agent_name";
	public static final String REACTION_TIME = "reaction_time";
	public static final String ACTION_NUMBER = "action_number";
	public static final String GAME_NUMBER = "game_number";
	public static final String COMMA_DELIMITER = ",";
	public static final String CLIENT_ID = "client_id";
	public static final String URL_ID = "url_id";

	private StringBuffer actionRecord = new StringBuffer(""); 
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

	private GridGameManager server;

	/**
	 * The connected users current score. This is probably not where this should be tracked.
	 */
	private double currentScore;
	private SGDomain domain;
	
	private final String turkId;

	public GameHandler(GridGameManager server, Session session, String threadId, String turkId) {
		this.server = server;
		this.session = session;
		this.threadId = threadId;
		this.currentScore = 0.0;
		this.turkId = turkId;
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
			}else if(msgType.equals(GridGameManager.REACTION_TIME)){
				this.appendToActionRecord(msg);

			} else if (msgType.equals(TAKE_ACTION)) {
				if (this.agent.isGameStarted()) {
					String actionName = msg.getString(ACTION);
					Action groundedAction = new SimpleAction(actionName);
					this.agent.setNextAction(groundedAction);
					response.setString(RESULT, SUCCESS);
				} else {
					response.setString("Game_started", "Game was not started");
					response.setError(true);
				}
			}else if(msgType.equals(INITIALIZE)){

			}else if (msgType.equals(HEARTBEAT)){
				
			}else {
				System.out.println("Unhandled message type: " + msgType);
				
			}
		} catch (TokenCastException e) {
			
			System.out.flush();
			response.setError(true);
		}
	}

	private void appendToActionRecord(GridGameServerToken msg) {
		String agent_name;
		try {
			agent_name = msg.getString(AGENT_NAME);
			String client_id = msg.getString(CLIENT_ID);
			String url_id = msg.getString(URL_ID);
			
			int reaction_time = msg.getInt(REACTION_TIME);
			int action_number = msg.getInt(ACTION_NUMBER);
			int game_number = msg.getInt(GAME_NUMBER);
			
			this.actionRecord.append(client_id).append(COMMA_DELIMITER).append(agent_name).append(COMMA_DELIMITER);
			this.actionRecord.append(url_id).append(COMMA_DELIMITER).append(game_number).append(COMMA_DELIMITER);
			this.actionRecord.append(action_number).append(COMMA_DELIMITER).append(reaction_time).append("\n");
		} catch (TokenCastException e) {
			e.printStackTrace();
		}
	}

	/** 
	 * When the NetworkAgent needs a new action, it can ask the client for a new action.
	 * @param s
	 */
	public void begForAction(State s) {
		GridGameServerToken request = new GridGameServerToken();
		request.setString(GridGameManager.MSG_TYPE, ACTION_REQUEST);
		request.setState(STATE, s);
		this.updateClient(request);
	}

	/**
	 * When any update happens to the state, the client needs to be notified.
	 * @param state
	 */
	public void updateClient(State state) {
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, state);
		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(GridGameManager.MSG_TYPE, UPDATE);
		this.updateClient(msg);
	}

	public String getParticipantId() {
		return this.turkId;
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
			double[] jointReward, State sprime, boolean isTerminal) {
		
		this.currentScore += jointReward[this.agent.getAgentNum()];
		GridGameServerToken token = new GridGameServerToken();
		token.setState(STATE, sprime);
		token.setDouble(SCORE, this.currentScore);
		token.setToken(ACTION, GridGameServerToken.tokenFromJointAction(jointAction));
		token.setObject(REWARD, jointReward);
		token.setBoolean(IS_TERMINAL, isTerminal);

		GridGameServerToken msg = new GridGameServerToken();
		msg.setToken(UPDATE, token);
		msg.setString(GridGameManager.MSG_TYPE, UPDATE);
		this.updateClient(msg);
	}

	/**
	 * If any other message needs to be sent to the client, it is send here.
	 * @param message
	 */
	public void updateClient(GridGameServerToken message) {
		if (this.session.isOpen()) {
			this.session.getRemote().sendStringByFuture(message.toJSONString());
		}
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
		System.out.println("SHUTTING DOWN");
		GridGameServerToken msg = new GridGameServerToken();
		msg.setString(GridGameManager.MSG_TYPE, EXPERIMENT_COMPLETED);
		msg.setDouble(SCORE, this.currentScore);

		this.updateClient(msg);
		
		this.session.close();
	}

	/**
	 * Adds a new NetworkAgent to the world.
	 * @param world
	 */
	public void addNetworkAgent(World world) {
		this.agent = NetworkAgent.getNetworkAgent(this);
		this.domain = world.getDomain();

		SGAgentType agentType = 
				new SGAgentType(GridGame.CLASS_AGENT , this.domain.getActionTypes());

		world.join(this.agent);
	}

	public boolean isConnected() {
		return this.session.isOpen();
	}

	public NetworkAgent getNetworkAgent(){
		return agent;
	}

	public String getThreadId(){
		return threadId;
	}

	public String getActionRecord() {

		return actionRecord.toString();
	}

	public void clearActionRecord() {
		actionRecord.setLength(0);

	}

}
