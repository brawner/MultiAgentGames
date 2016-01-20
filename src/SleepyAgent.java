import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.agentactions.GroundedSGAgentAction;


public class SleepyAgent extends RandomSGAgent {
	private double sleepyTime;
	public SleepyAgent(double sleepyTime) {
		super();
		this.sleepyTime = sleepyTime;
	}
	
	@Override
	public boolean canBeThreaded() {
		return true;
	}

	@Override
	public GroundedSGAgentAction getAction(State s) {
		GroundedSGAgentAction randomAction = super.getAction(s);
		try {
			Thread.sleep((long)(this.sleepyTime * 1000));
		} catch (InterruptedException e) {
			return null;
		}
		return randomAction;
	}

}
