package networking.server;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;


public class NetworkAgent extends Agent {
	private GroundedSingleAction currentAction;
	private ReentrantReadWriteLock lock;
	private final GameHandler handler;
	public NetworkAgent(GameHandler handler) {
		this.handler = handler;
		this.lock = new ReentrantReadWriteLock();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void gameStarting() {
		
	}
	
	@Override
	public void gameStarting(State startState) {
		this.handler.updateClient(startState);
	}

	@Override
	public GroundedSingleAction getAction(State s) {
		this.lock.readLock().lock();
		GroundedSingleAction action = this.currentAction;
		this.lock.readLock().unlock();
		
		if (action == null) {
			this.handler.begForAction(s);
		}
		
		try {
			int count = 0;
			while (action == null && this.world.isOk()) {
				this.lock.readLock().lock();
				action = this.currentAction;
				this.lock.readLock().unlock();
				Thread.sleep(10);	
				if (count++ == 100) {
					count = 0;
					System.out.println("Waiting on action for agent " + this.worldAgentName);
				}
				
			}
		} catch (InterruptedException e) {
		}
		
		this.lock.writeLock().lock();
		this.currentAction = null;
		this.lock.writeLock().unlock();
		
		return action;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		this.currentAction = null;
		this.handler.updateClient(s, jointAction, jointReward, sprime, isTerminal);

	}

	@Override
	public void gameTerminated() {
		this.handler.gameCompleted();

	}
	
	public void setNextAction(GroundedSingleAction action) {
		this.lock.writeLock().lock();
		this.currentAction = action;
		this.lock.writeLock().unlock();
	}

}
