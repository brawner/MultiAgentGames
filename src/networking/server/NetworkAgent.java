package networking.server;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;

/**
 * A subclass of the StochasticGames agent. This implements the requried methods for the World to call when running a game
 * @author brawner
 *
 */
public class NetworkAgent extends Agent {
	/**
	 * The current action specified by the connected person
	 */
	private GroundedSingleAction currentAction;
	
	/**
	 * To protect concurrent access
	 */
	private ReentrantReadWriteLock lock;
	
	/**
	 * The associated game handler.
	 */
	private final GameHandler handler;
	
	
	public NetworkAgent(GameHandler handler) {
		this.handler = handler;
		this.lock = new ReentrantReadWriteLock();
	}

	/**
	 * Basic game starting method, not used here.
	 */
	@Override
	public void gameStarting() {
		
	}
	
	/**
	 * The updated game starting method. This overrides Agent's default implementation, and actually updates the client with the new state.
	 */
	@Override
	public void gameStarting(State startState) {
		this.handler.updateClient(startState);
	}

	/**
	 * Attempts to get an action from the connected user. If no action has been specified, then it asks the user for a new action.
	 */
	@Override
	public GroundedSingleAction getAction(State s) {
		this.lock.readLock().lock();
		GroundedSingleAction action = this.currentAction;
		this.lock.readLock().unlock();
		
		if (action == null) {
			this.handler.begForAction(s);
		}
		while (action == null && this.world.isOk() && this.handler.isConnected()) {
			this.lock.readLock().lock();
			action = this.currentAction;
			this.lock.readLock().unlock();	
		}
		this.lock.writeLock().lock();
		this.currentAction = null;
		this.lock.writeLock().unlock();
		
		return action;
	}

	/**
	 * Updates the client with the observed outcome of a world. The state will be abstracted for this particular agent.
	 */
	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		this.currentAction = null;
		this.handler.updateClient(s, jointAction, jointReward, sprime, isTerminal);

	}

	/**
	 * Checks if the game has been terminated.
	 */
	@Override
	public void gameTerminated() {
		this.handler.gameCompleted();

	}
	
	/**
	 * Sets the client's requested action for the next turn.
	 * @param action
	 */
	public void setNextAction(GroundedSingleAction action) {
		this.lock.writeLock().lock();
		this.currentAction = action;
		this.lock.writeLock().unlock();
	}

}
