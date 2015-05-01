import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;


public class SleepyAgent extends RandomAgent {
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
	public GroundedSingleAction getAction(State s) {
		GroundedSingleAction randomAction = super.getAction(s);
		try {
			Thread.sleep((long)(this.sleepyTime * 1000));
		} catch (InterruptedException e) {
			return null;
		}
		return randomAction;
	}

}
