package networking.common;

import java.util.ArrayList;
import java.util.List;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.StateAbstraction;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.SGAgent;

/**
 * Abstracts the goals away, so that an agent cannot tell which goal another agent is attempting to go to.
 * @author brawner
 *
 */
public class GoalAbstraction implements StateAbstraction{
	private State reference;
	public GoalAbstraction(State reference) {
		this.reference = reference.copy();
	}
	@Override
	public State abstraction(State s) {
		List<ObjectInstance> goalObjects = this.reference.getObjectsOfClass(GridGame.CLASSGOAL);
		List<ObjectInstance> goalsToRemove = s.getObjectsOfClass(GridGame.CLASSGOAL);
		if (goalObjects.equals(goalsToRemove)) {
			return s;
		}
		
		
		State abstracted = s.copy();
		for (ObjectInstance goal : goalsToRemove) {
			abstracted.removeObject(goal);
		}
		for (ObjectInstance goal : goalObjects) {
			abstracted.addObject(goal);
		}
		return abstracted;
	}
	
	/**
	 * Replaces the goal objects with the goals from the reference state, except for the goals that match the agent's number
	 * and do not exist in the current state.
	 */
	public State abstraction(State state, SGAgent agent) {
		// Copy state
		State abstracted = state.copy();
		
		// The complete list of goals
		List<ObjectInstance> goalObjects = this.reference.getObjectsOfClass(GridGame.CLASSGOAL);
		
		// The list of actual goals
		List<ObjectInstance> actualGoals = state.getObjectsOfClass(GridGame.CLASSGOAL);
		
		// Goals from the reference state, that shouldn't be added back in because they match the agent's player number
		// and do not exist in the current state
		List<ObjectInstance> goalsNotToAdd = new ArrayList<ObjectInstance>();
		
		ObjectInstance agentObject = abstracted.getObject(agent.getAgentName());
		int playerNum = agentObject.getIntValForAttribute(GridGame.ATTPN);
		
		// Iterate through all the goal objects
		for (ObjectInstance goalToAdd : goalObjects) {
			
			// If the goal type doesn't match this player number, then it should be added regardless
			int goalType = goalToAdd.getIntValForAttribute(GridGame.ATTGT);
			if (goalType != playerNum + 1) {
				continue;
			}
			
			// If there is no goal that matches the goalToAdd, then the goalToAdd should not be added to the abstracted state
			boolean found = false;
			for (ObjectInstance actualGoal : actualGoals) {
				if (goalToAdd.valueEquals(actualGoal)) {
					found = true;
					break;
				}
			}
			if (!found) {
				goalsNotToAdd.add(goalToAdd);
			}
		}
		
		if (actualGoals.equals(goalObjects)) {
			return abstracted;
		}
		
		// Remove all the goals from the state
		for (ObjectInstance goal : actualGoals) {
			abstracted.removeObject(goal);
		}
		
		// Add in all the goals that need to be added
		for (ObjectInstance goal : goalObjects) {
			if (!goalsNotToAdd.contains(goal)) {
				abstracted.addObject(goal);
			}
		}
		return abstracted;
	}

}
