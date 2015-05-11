package networking.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import networking.common.GridGameWorldLoader.WorldLoaderException;
import networking.common.messages.WorldFile;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;

public class WorldLoadingStateGenerator extends SGStateGenerator{
	private final State allGoalsUnsetAgents;
	private final State prunedGoalsUnsetAgents;
	
	private WorldLoadingStateGenerator(State allGoalsUnsetAgents, State prunedGoalsUnsetAgents) {
		super();
		this.allGoalsUnsetAgents = allGoalsUnsetAgents;
		this.prunedGoalsUnsetAgents = prunedGoalsUnsetAgents;
	}
	
	private static List<List<Integer>> pruneGoals(List<List<Integer>> goals, int goalsPerAgent) {
		if (goalsPerAgent == 0) {
			return goals;
		}
		
		List<List<Integer>> res = new ArrayList<List<Integer>>();
		Map<Integer, List<List<Integer>>> goalsByAgent = new HashMap<Integer, List<List<Integer>>>();
		for (List<Integer> goal : goals) {
			int goalType = goal.get(2);
			List<List<Integer>> agentsGoals = goalsByAgent.get(goalType);
			if (agentsGoals == null) {
				agentsGoals = new ArrayList<List<Integer>>();
				goalsByAgent.put(goalType, agentsGoals);
			}
			agentsGoals.add(goal);
		}
		
		for (Map.Entry<Integer, List<List<Integer>>> entry : goalsByAgent.entrySet()) {
			List<List<Integer>> list = entry.getValue();
			Collections.shuffle(list);
			for (int i = 0; i < goalsPerAgent && i < list.size(); i++) {
				res.add(list.get(i));
			}
		}
		
		return res;
	}
	
	public static WorldLoadingStateGenerator stateGenerator(GridGameServerToken fileToken, final SGDomain domain, int goalsPerAgent) throws TokenCastException {
		Integer width = fileToken.getInt(WorldFile.WIDTH);
		Integer height = fileToken.getInt(WorldFile.HEIGHT);
		List<GridGameServerToken> agentObjects = fileToken.getTokenList(WorldFile.AGENTS);
		List<GridGameServerToken> goalObjects = fileToken.getTokenList(WorldFile.GOALS);
		List<GridGameServerToken> horizontalWallObjects = fileToken.getTokenList(WorldFile.HORIZONTAL_WALLS);
		List<GridGameServerToken> verticalWallObjects = fileToken.getTokenList(WorldFile.VERTICAL_WALLS);
		List<GridGameServerToken> rewardObjects = fileToken.getTokenList(WorldFile.REWARDS);
		if (agentObjects == null) {
			throw new WorldLoaderException("Agents are not properly specified");
		}
		if (goalObjects == null) {
			throw new WorldLoaderException("Goals are not properly specified");
		}
		
		final List<List<Integer>> agentPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken agentObj : agentObjects) {
			Integer startX = agentObj.getInt(GridGame.ATTX);
			Integer startY = agentObj.getInt(GridGame.ATTY);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			agentPositions.add(Arrays.asList(startX, startY));
		}
		
		final List<List<Integer>> goalPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken goalObj : goalObjects) {
			Integer startX = goalObj.getInt(GridGame.ATTX);
			Integer startY = goalObj.getInt(GridGame.ATTY);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			Integer goalType = goalObj.getInt(GridGame.ATTGT);
			if (goalType == null) {
				goalType = 0;
			}
			goalPositions.add(Arrays.asList(startX, startY, goalType));
		}
		List<List<Integer>> prunedGoalPositions = WorldLoadingStateGenerator.pruneGoals(goalPositions, goalsPerAgent);
		
		
		final List<List<Integer>> horizontalWallPositions = new ArrayList<List<Integer>>();
		if (horizontalWallObjects != null) {
			for (GridGameServerToken wallObject : horizontalWallObjects) {
				Integer startX = wallObject.getInt(GridGame.ATTE1);
				Integer endX = wallObject.getInt(GridGame.ATTE2);
				Integer startY = wallObject.getInt(GridGame.ATTP);
				Integer wallType = wallObject.getInt(GridGame.ATTWT);
				if (startX == null || endX == null || startY == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				if (wallType == null) {
					wallType = 0;
				}
				horizontalWallPositions.add(Arrays.asList(startX, endX, startY, wallType));
			}
		}
		
		
		final List<List<Integer>> verticalWallPositions = new ArrayList<List<Integer>>();
		if (verticalWallObjects != null) {
			for (GridGameServerToken wallObject : verticalWallObjects) {
				Integer startX = wallObject.getInt(GridGame.ATTP);
				Integer startY = wallObject.getInt(GridGame.ATTE1);
				Integer endY = wallObject.getInt(GridGame.ATTE2);
				if (startX == null || startY == null || endY == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				Integer wallType = wallObject.getInt(GridGame.ATTWT);
				if (wallType == null) {
					wallType = 0;
				}
				verticalWallPositions.add(Arrays.asList(startX, startY, endY, wallType));
			}
		}
		
		final List<List<Integer>> rewardPositions = new ArrayList<List<Integer>>();
		if (rewardObjects != null) {
			for (GridGameServerToken rewardObject : rewardObjects) {
				Integer x = rewardObject.getInt(GridGame.ATTX);
				Integer y = rewardObject.getInt(GridGame.ATTY);
				Integer cost = rewardObject.getInt(GridGameExtreme.ATTVALUE);
				if (x == null || y == null || cost == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				rewardPositions.add(Arrays.asList(x, y, cost));
			}
		}
		State allGoalsUnsetAgents =  WorldLoadingStateGenerator.generateBaseState(domain, agentPositions, goalPositions, 
				horizontalWallPositions, verticalWallPositions, rewardPositions, width, height);
		
		State prunedGoalsUnsetAgents = WorldLoadingStateGenerator.generateBaseState(domain, agentPositions, prunedGoalPositions, 
				horizontalWallPositions, verticalWallPositions, rewardPositions, width, height);
		
		return new WorldLoadingStateGenerator(allGoalsUnsetAgents, prunedGoalsUnsetAgents);
	}
	
	private static State generateBaseState(SGDomain domain, List<List<Integer>> agentPositions, List<List<Integer>> goalPositions,
			List<List<Integer>> horizontalWalls, List<List<Integer>> verticalWalls, List<List<Integer>> rewards, int width, int height) {
		
		State state = GridGame.getCleanState(domain, agentPositions.size(), goalPositions.size(), horizontalWalls.size() + 2, verticalWalls.size() + 2, width, height);
		
		for (int i = 0; i < agentPositions.size(); i++) {
			List<Integer> positions = agentPositions.get(i);
			int x = positions.get(0);
			int y = positions.get(1);
			GridGame.setAgent(state, i, x, y, i);
		}
		
		
		for (int i = 0; i < goalPositions.size(); i++) {
			List<Integer> goal = goalPositions.get(i);
			int x = goal.get(0);
			int y = goal.get(1);
			int goalType = goal.get(2);
			GridGame.setGoal(state, i, x, y, goalType);
		}
		
		for (int i = 0 ; i < horizontalWalls.size(); i++) {
			List<Integer> wall = horizontalWalls.get(i);
			int x1 = wall.get(0);
			int x2 = wall.get(1);
			int y = wall.get(2);
			int wallType = wall.get(3);
			GridGame.setHorizontalWall(state, i + 2, y, x1, x2, wallType);
		}
		
		for (int i = 0 ; i < verticalWalls.size(); i++) {
			List<Integer> wall = verticalWalls.get(i);
			int x = wall.get(0);
			int y1 = wall.get(1);
			int y2 = wall.get(2);
			int wallType = wall.get(3);

			GridGame.setVerticalWall(state, i + 2, x, y1, y2, wallType);
		}
		
		for (int i = 0; i < rewards.size(); i++) {
			List<Integer> reward = rewards.get(i);
			int x = reward.get(0);
			int y = reward.get(1);
			int value = reward.get(2);
			ObjectClass rewardClass = domain.getObjectClass(GridGameExtreme.CLASSREWARD);
			ObjectInstance rewardObject = new ObjectInstance(rewardClass, "reward" + i);
			rewardObject.setValue(GridGame.ATTX, x);
			rewardObject.setValue(GridGame.ATTY, y);
			rewardObject.setValue(GridGameExtreme.ATTVALUE, value);
			state.addObject(rewardObject);
		}
		
		return state;
	}

	@Override
	public State generateState(List<Agent> agents) {
		
		State copy = this.prunedGoalsUnsetAgents.copy();
		List<ObjectInstance> agentObjects = copy.getObjectsOfClass(GridGame.CLASSAGENT);
		for (int i = 0; i < agentObjects.size() && i < agents.size(); i++) {
			Agent agent = agents.get(i);
			ObjectInstance agentObject = agentObjects.get(i);
			copy.renameObject(agentObject.getName(), agent.getAgentName());
		}
		return copy;
	}
	
	public State generateAbstractedState() {
		return this.allGoalsUnsetAgents;
	}
	
	public List<ObjectInstance> getGoalsForAgent(State state, Agent agent) {
		ObjectInstance agentObject = state.getObject(agent.getAgentName());
		if (agentObject == null) {
			return null;
		}
		
		return this.getGoalsForAgent(agentObject.getDiscValForAttribute(GridGame.ATTPN));
	}
	
	public List<ObjectInstance> getGoalsForAgent(int playerNum) {
		List<ObjectInstance> goalObjects = this.prunedGoalsUnsetAgents.getObjectsOfClass(GridGame.CLASSGOAL);
		List<ObjectInstance> goalsForAgent = new ArrayList<ObjectInstance>();
		for (ObjectInstance goal : goalObjects) {
			int goalType = goal.getDiscValForAttribute(GridGame.ATTGT);
			if ( goalType == 0 || goalType == playerNum + 1) {
				goalObjects.add(goal);
			}
		}
		return goalsForAgent;
	}

}
