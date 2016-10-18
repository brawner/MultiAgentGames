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
import burlap.domain.stochasticgames.gridgame.state.GGAgent;
import burlap.domain.stochasticgames.gridgame.state.GGGoal;
import burlap.domain.stochasticgames.gridgame.state.GGWall.GGHorizontalWall;
import burlap.domain.stochasticgames.gridgame.state.GGWall.GGVerticalWall;
import burlap.mdp.auxiliary.StateGenerator;
import burlap.mdp.core.oo.state.MutableOOState;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.oo.state.generic.GenericOOState;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgent;

public class WorldLoadingStateGenerator implements StateGenerator{
	private final OOState allGoalsUnsetAgents;
	private final OOState noGoalsUnsetAgents;
	private final int goalsPerAgent;
	
	private WorldLoadingStateGenerator(OOState allGoalsUnsetAgents, OOState noGoalsUnsetAgents, int goalsPerAgent) {
		super();
		this.allGoalsUnsetAgents = allGoalsUnsetAgents;
		this.noGoalsUnsetAgents = noGoalsUnsetAgents;
		this.goalsPerAgent = goalsPerAgent;
	}
	
	private static List<ObjectInstance> pruneGoals(List<ObjectInstance> goals, int goalsPerAgent) {
		if (goalsPerAgent == 0) {
			return goals;
		}
		
		List<ObjectInstance> res = new ArrayList<ObjectInstance>();
		Map<Integer, List<ObjectInstance>> goalsByAgent = new HashMap<Integer, List<ObjectInstance>>();
		for (ObjectInstance goal : goals) {
			int goalType = (Integer)goal.get(GridGame.VAR_GT);
			List<ObjectInstance> agentsGoals = goalsByAgent.get(goalType);
			if (agentsGoals == null) {
				agentsGoals = new ArrayList<ObjectInstance>();
				goalsByAgent.put(goalType, agentsGoals);
			}
			agentsGoals.add((ObjectInstance)goal.copy());
		}
		
		for (Map.Entry<Integer, List<ObjectInstance>> entry : goalsByAgent.entrySet()) {
			List<ObjectInstance> list = entry.getValue();
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
		if (agentObjects == null) {
			throw new WorldLoaderException("Agents are not properly specified");
		}
		if (goalObjects == null) {
			throw new WorldLoaderException("Goals are not properly specified");
		}
		
		final List<List<Integer>> agentPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken agentObj : agentObjects) {
			Integer startX = agentObj.getInt(GridGame.VAR_X);
			Integer startY = agentObj.getInt(GridGame.VAR_Y);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			agentPositions.add(Arrays.asList(startX, startY));
		}
		
		final List<List<Integer>> goalPositions = new ArrayList<List<Integer>>();
		for (GridGameServerToken goalObj : goalObjects) {
			Integer startX = goalObj.getInt(GridGame.VAR_X);
			Integer startY = goalObj.getInt(GridGame.VAR_Y);
			if (startX == null || startY == null) {
				throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
			}
			Integer goalType = goalObj.getInt(GridGame.VAR_GT);
			if (goalType == null) {
				goalType = 0;
			}
			goalPositions.add(Arrays.asList(startX, startY, goalType));
		}
		
		final List<List<Integer>> horizontalWallPositions = new ArrayList<List<Integer>>();
		if (horizontalWallObjects != null) {
			for (GridGameServerToken wallObject : horizontalWallObjects) {
				Integer startX = wallObject.getInt(GridGame.VAR_E1);
				Integer endX = wallObject.getInt(GridGame.VAR_E2);
				Integer startY = wallObject.getInt(GridGame.VAR_POS);
				Integer wallType = wallObject.getInt(GridGame.VAR_WT);
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
				Integer startX = wallObject.getInt(GridGame.VAR_POS);
				Integer startY = wallObject.getInt(GridGame.VAR_E1);
				Integer endY = wallObject.getInt(GridGame.VAR_E2);
				if (startX == null || startY == null || endY == null) {
					throw new RuntimeException("Parsing file token failed\n" + fileToken.toJSONString());
				}
				Integer wallType = wallObject.getInt(GridGame.VAR_WT);
				if (wallType == null) {
					wallType = 0;
				}
				verticalWallPositions.add(Arrays.asList(startX, startY, endY, wallType));
			}
		}
		
		final List<List<Integer>> rewardPositions = new ArrayList<List<Integer>>();
		OOState allGoalsUnsetAgents =  WorldLoadingStateGenerator.generateBaseState(domain, agentPositions, goalPositions, 
				horizontalWallPositions, verticalWallPositions, rewardPositions, width, height);
		
		OOState noGoalsUnsetAgents = WorldLoadingStateGenerator.generateBaseState(domain, agentPositions, null, 
				horizontalWallPositions, verticalWallPositions, rewardPositions, width, height);
		
		return new WorldLoadingStateGenerator(allGoalsUnsetAgents, noGoalsUnsetAgents, goalsPerAgent);
	}
	
	private static OOState generateBaseState(SGDomain domain, List<List<Integer>> agentPositions, List<List<Integer>> goalPositions,
			List<List<Integer>> horizontalWalls, List<List<Integer>> verticalWalls, List<List<Integer>> rewards, int width, int height) {
		List<ObjectInstance> stateObjects = new ArrayList<ObjectInstance>();
		
		for (int i = 0; i < agentPositions.size(); i++) {
			List<Integer> positions = agentPositions.get(i);
			int x = positions.get(0);
			int y = positions.get(1);
			stateObjects.add(new GGAgent(x, y, i, "agent" + i));
		}
		
		if (goalPositions != null) {
			for (int i = 0; i < goalPositions.size(); i++) {
				List<Integer> goal = goalPositions.get(i);
				int x = goal.get(0);
				int y = goal.get(1);
				int goalType = goal.get(2);
				stateObjects.add(new GGGoal(x, y, goalType, "goal" + i));
			}
		}
		
		for (int i = 0 ; i < horizontalWalls.size(); i++) {
			List<Integer> wall = horizontalWalls.get(i);
			int x1 = wall.get(0);
			int x2 = wall.get(1);
			int y = wall.get(2);
			int wallType = wall.get(3);
			stateObjects.add(new GGHorizontalWall(x1, x2, y, wallType, "hwall" + i));
		}
		
		for (int i = 0 ; i < verticalWalls.size(); i++) {
			List<Integer> wall = verticalWalls.get(i);
			int x = wall.get(0);
			int y1 = wall.get(1);
			int y2 = wall.get(2);
			int wallType = wall.get(3);
			stateObjects.add(new GGVerticalWall(y1, y2, x, wallType, "vwall" + i));
		}
			
		GenericOOState state = new GenericOOState(stateObjects.toArray(new ObjectInstance[stateObjects.size()]));
				
		GridGame.setBoundaryWalls(state, width, height);
		
		
		
		return state;
	}

	public State generateState(List<SGAgent> agents) {
		
		MutableOOState copy = (MutableOOState)this.generateState();
		List<ObjectInstance> goals = this.allGoalsUnsetAgents.objectsOfClass(GridGame.CLASS_GOAL);
		List<ObjectInstance> goalsToAdd = WorldLoadingStateGenerator.pruneGoals(goals, this.goalsPerAgent);
		
		for (ObjectInstance goal : goalsToAdd) {
			copy = copy.addObject(goal);
		}
		
//		List<ObjectInstance> agentObjects = new ArrayList<ObjectInstance>(copy.objectsOfClass(GridGame.CLASS_AGENT));
//		for (int i = 0; i < agentObjects.size() && i < agents.size(); i++) {
//			SGAgent agent = agents.get(i);
//			ObjectInstance agentObject = agentObjects.get(i);
//			copy = copy.renameObject(agentObject.name(), agent.agentName());
//		}
		return copy;
	}
	
	public State generateAbstractedState() {
		return this.allGoalsUnsetAgents;
	}

	@Override
	public State generateState() {
		return this.noGoalsUnsetAgents.copy();
	}
	
	

}
