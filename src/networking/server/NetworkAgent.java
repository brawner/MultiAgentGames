package networking.server;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.agentactions.GroundedSGAgentAction;

/**
 * A subclass of the StochasticGames agent. This implements the required methods for the World to call when running a game
 * @author brawner
 *
 */
public class NetworkAgent extends SGAgent {
	/**
	 * The current action specified by the connected person
	 */
	private GroundedSGAgentAction currentAction;
	
	/**
	 * The associated game handler.
	 */
	private final GameHandler handler;
	
	private Boolean gameStarted;
	
	private static Map<GameHandler, NetworkAgent> agentMap = 
			Collections.synchronizedMap(new HashMap<GameHandler, NetworkAgent>());
	
	private NetworkAgent(GameHandler handler) {
		this.handler = handler;
		this.gameStarted = false;
	}
	
	public static NetworkAgent getNetworkAgent(GameHandler handler) {
		synchronized(NetworkAgent.agentMap) {
			NetworkAgent agent = NetworkAgent.agentMap.get(handler);
			if (agent == null) {
				agent = new NetworkAgent(handler);
				NetworkAgent.agentMap.put(handler, agent);
			}
			return agent;
		}
	}

	/**
	 * Basic game starting method, not used here.
	 */
	@Override
	public void gameStarting() {
		this.gameStarted = true;
	}
	
	public Boolean isGameStarted() {
		return this.gameStarted;
	}
	
	/**
	 * The updated game starting method. This overrides Agent's default implementation, and actually updates the client with the new state.
	 */
	@Override
	public void gameStarting(State startState) {
		this.handler.updateClient(startState);
		this.gameStarted = true;
	}

	/**
	 * Attempts to get an action from the connected user. If no action has been specified, then it asks the user for a new action.
	 */
	@Override
	public GroundedSGAgentAction getAction(State s) {
		GroundedSGAgentAction action = null;
		synchronized(this) {
			action = this.currentAction;
		}
		
		if (action == null) {
			this.handler.begForAction(s);
		}
		while (action == null && this.world.isOk() && this.handler.isConnected()) {
			synchronized(this) {
				action = this.currentAction;
			}
		}
		synchronized(this) {
			this.currentAction = null;
		}
		
		return action;
	}

	/**
	 * Updates the client with the observed outcome of a world. The state will be abstracted for this particular agent.
	 */
	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		this.currentAction = null;
		//System.out.println("Is terminal: "+isTerminal);
		this.handler.updateClient(s, jointAction, jointReward, sprime, isTerminal);

	}

	/**
	 * Checks if the game has been terminated.
	 */
	@Override
	public void gameTerminated() {
		this.handler.gameCompleted();
		this.gameStarted = false;
	}
	
	/**
	 * Sets the client's requested action for the next turn.
	 * @param action
	 */
	public void setNextAction(GroundedSGAgentAction action) {
		synchronized(this) {
			this.currentAction = action;
		}
	}

}
