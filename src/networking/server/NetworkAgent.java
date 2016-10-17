package networking.server;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import burlap.domain.stochasticdomain.world.NetworkWorld;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgentBase;
import burlap.mdp.stochasticgames.world.World;

/**
 * A subclass of the StochasticGames agent. This implements the required methods for the World to call when running a game
 * @author brawner
 *
 */
public class NetworkAgent extends SGAgentBase {
	/**
	 * The current action specified by the connected person
	 */
	private Action currentAction;
	
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
	public void gameStarting(World w, int i) {
		NetworkWorld nw = (NetworkWorld)w;
		if (nw.getCurrentWorldState() == null) {
			nw.generateNewCurrentState();
		}
		State state = nw.getCurrentWorldState();
		this.handler.updateClient(state);
		this.gameStarted = true;
	}
	
	public Boolean isGameStarted() {
		return this.gameStarted;
	}
	
	/**
	 * The updated game starting method. This overrides Agent's default implementation, and actually updates the client with the new state.
	 */

	/**
	 * Attempts to get an action from the connected user. If no action has been specified, then it asks the user for a new action.
	 */


	/**
	 * Updates the client with the observed outcome of a world. The state will be abstracted for this particular agent.
	 */

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			double[] jointReward, State sprime, boolean isTerminal) {
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
	public void setNextAction(Action action) {
		synchronized(this) {
			this.currentAction = action;
		}
	}

	@Override
	public Action action(State s) {
		Action action = null;
		synchronized(this) {
			action = this.currentAction;
		}
		
		if (action == null) {
			this.handler.begForAction(s);
		}
		while (action == null && ((NetworkWorld)this.world).isOk() && this.handler.isConnected()) {
			synchronized(this) {
				action = this.currentAction;
			}
		}
		synchronized(this) {
			this.currentAction = null;
		}
		
		return action;
	}

	public int getAgentNum() {
		return -1;
	}
}
