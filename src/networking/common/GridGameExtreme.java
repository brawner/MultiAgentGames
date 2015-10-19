package networking.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanicsWithoutTieBreaking;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.visualizer.RenderLayer;
import burlap.oomdp.visualizer.Visualizer;

public class GridGameExtreme {
	public static String CLASSREWARD = "reward";
	public static String ATTVALUE = "value";
	public static String PFINREWARD = "in_reward";
	
	public static SGDomain generateDomain(GridGame gridGame, boolean randomlyBreakTies) {
		//gridGame.setMaxDim(5);
		SGDomain domain = (SGDomain) gridGame.generateDomain();
		if (!randomlyBreakTies) {
			domain.setJointActionModel(new GridGameStandardMechanicsWithoutTieBreaking(domain, gridGame.getSemiWallProb()));
		}
		
		Attribute xAtt = domain.getAttribute(GridGame.ATTX);
		Attribute yAtt = domain.getAttribute(GridGame.ATTY);
		Attribute cost = new Attribute(domain, ATTVALUE, Attribute.AttributeType.INT);
		cost.setDiscValuesForRange(-Integer.MAX_VALUE, -Integer.MAX_VALUE, 1);
		domain.addAttribute(cost);
		ObjectClass classToll = new ObjectClass(domain, CLASSREWARD);
		classToll.addAttribute(xAtt);
		classToll.addAttribute(yAtt);
		classToll.addAttribute(cost);
		
		AgentInReward agentInReward = new AgentInReward(PFINREWARD, domain);
		return domain;
	}
	
	public static SGDomain generateDomain(GridGame gridGame) {
		return generateDomain(gridGame, true);
	}
	
	public static JointActionModel generateJointActionModel(Domain domain) {
		return new GridGameStandardMechanics(domain);
	}
	
	public static TerminalFunction generateTerminalFunction(SGDomain domain) {
		return new GridGame.GGTerminalFunction(domain);
	}
	
	// TODO change joint reward function
	public static JointReward generateJointReward(SGDomain domain) {
		return new GridGame.GGJointRewardFunction(domain);
	};

	public static JointReward generateJointReward(SGDomain domain, double stepCost, double reward, boolean incurCostOnNoop) {
		return new GridGame.GGJointRewardFunction(domain, stepCost, reward, incurCostOnNoop);
	};
	
	public static Visualizer getVisualizer(World world) {
		State startState = world.startingState();
		List<ObjectInstance> horizontalWalls = startState.getObjectsOfClass(GridGame.CLASSDIMHWALL);
		List<ObjectInstance> verticalWalls = startState.getObjectsOfClass(GridGame.CLASSDIMVWALL);
		if (horizontalWalls.size() < 2 || verticalWalls.size() < 2) {
			throw new RuntimeException("This world does not have valid walls");
		}
		
		ObjectInstance leftWall = verticalWalls.get(0);
		ObjectInstance rightWall = verticalWalls.get(1);
		ObjectInstance bottomWall = horizontalWalls.get(0);
		ObjectInstance topWall = horizontalWalls.get(1);
		
		final int cWidth = rightWall.getIntValForAttribute(GridGame.ATTP) - leftWall.getIntValForAttribute(GridGame.ATTP);
		final int cHeight = topWall.getIntValForAttribute(GridGame.ATTP) - bottomWall.getIntValForAttribute(GridGame.ATTP); 
		
		Visualizer visualizer = GGVisualizer.getVisualizer(cWidth, cHeight);
		List<Color> colors = Arrays.asList(Color.RED);
		
		RenderLayer layer = new RenderLayer(){

			@Override
			public void render(Graphics2D g2, float width, float height) {
				BasicStroke s = new BasicStroke(2.0f);
				g2.setStroke(s);
				float columnWidth = width / cWidth;
				float columnHeight = height / cHeight;
				
				for (int i = 0; i < width; i++) {
					int x = (int)(i*columnWidth);
					Point bottom = new Point(x, 0);
					Point top = new Point(x, (int)height);
					g2.draw(new java.awt.geom.Line2D.Float(bottom, top));

				}
				for (int i = 0; i < width; i++) {
					int y = (int) (i * columnHeight);
					Point left = new Point( 0, y);
					Point right = new Point((int) width, y);
					g2.draw(new java.awt.geom.Line2D.Float(left, right));

				}
			}
		};
		visualizer.addRenderLayer(layer);
		visualizer.insertObjectClassPainter(0, GridGameExtreme.CLASSREWARD, new GGVisualizer.CellPainter(cWidth,cHeight, colors, 0));
		return visualizer;
	}
	
	/**
	 * Specifies goal rewards and default rewards for agents. Defaults rewards to 0 reward everywhere except transition to universal or personal goals which return a reward 1.
	 * @author James MacGlashan
	 *
	 */
	public static class GGJointRewardFunction implements JointReward {

		PropositionalFunction agentInPersonalGoal;
		PropositionalFunction agentInUniversalGoal;
		PropositionalFunction agentInRewardGrid;
		
		double stepCost = 0.;
		double pGoalReward = 1.;
		double uGoalReward = 1.;
		boolean noopIncursCost = false;
		Map<Integer, Double> personalGoalRewards = null;
		
		/**
		 * Initializes for a given domain. Defaults rewards to 0 reward everywhere except transition to unviersal or personal goals which return a reward 1.
		 * @param ggDomain the domain
		 */
		public GGJointRewardFunction(Domain ggDomain){
			agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
			agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
			agentInRewardGrid = ggDomain.getPropFunction(GridGameExtreme.PFINREWARD);
		}
		
		/**
		 * Initializes for a given domain, step cost reward and goal reward.
		 * @param ggDomain the domain
		 * @param stepCost the reward returned for all transitions except transtions to goal locations
		 * @param goalReward the reward returned for transitioning to a personal or universal goal
		 * @param noopIncursStepCost if true, then noop actions also incur the stepCost reward; if false, then noops always return 0 reward.
		 */
		public GGJointRewardFunction(Domain ggDomain, double stepCost, double goalReward, boolean noopIncursStepCost){
			agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
			agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
			agentInRewardGrid = ggDomain.getPropFunction(GridGameExtreme.PFINREWARD);
			this.stepCost = stepCost;
			this.pGoalReward = this.uGoalReward = goalReward;
			this.noopIncursCost = noopIncursStepCost;
		}
		
		
		/**
		 * Initializes for a given domain, step cost reward, personal goal reward, and universal goal reward.
		 * @param ggDomain the domain
		 * @param stepCost the reward returned for all transitions except transtions to goal locations
		 * @param personalGoalReward the reward returned for transitions to a personal goal
		 * @param universalGoalReward the reward returned for transitions to a universal goal
		 * @param noopIncursStepCost if true, then noop actions also incur the stepCost reward; if false, then noops always return 0 reward.
		 */
		public GGJointRewardFunction(Domain ggDomain, double stepCost, double personalGoalReward, double universalGoalReward, boolean noopIncursStepCost){
			agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
			agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
			agentInRewardGrid = ggDomain.getPropFunction(GridGameExtreme.PFINREWARD);
			this.stepCost = stepCost;
			this.pGoalReward = personalGoalReward;
			this.uGoalReward = universalGoalReward;
			this.noopIncursCost = noopIncursStepCost;
		}
		
		/**
		 * Initializes for a given domain, step cost reward, universal goal reward, and unique personal goal reward for each player.
		 * @param ggDomain the domain
		 * @param stepCost the reward returned for all transitions except transtions to goal locations
		 * @param universalGoalReward the reward returned for transitions to a universal goal
		 * @param noopIncursStepCost if true, then noop actions also incur the stepCost reward; if false, then noops always return 0 reward.
		 * @param personalGoalRewards a map from player numbers to their personal goal reward (the first player number is 0)
		 */
		public GGJointRewardFunction(Domain ggDomain, double stepCost, double universalGoalReward, boolean noopIncursStepCost, Map<Integer, Double> personalGoalRewards){
			
			agentInPersonalGoal = ggDomain.getPropFunction(GridGame.PFINPGOAL);
			agentInUniversalGoal = ggDomain.getPropFunction(GridGame.PFINUGOAL);
			agentInRewardGrid = ggDomain.getPropFunction(GridGameExtreme.PFINREWARD);
			this.stepCost = stepCost;
			this.uGoalReward = universalGoalReward;
			this.noopIncursCost = noopIncursStepCost;
			this.personalGoalRewards = personalGoalRewards;
			
		}
		
		@Override
		public Map<String, Double> reward(State s, JointAction ja, State sp) {
			
			Map <String, Double> rewards = new HashMap<String, Double>();
			
			//get all agents and initialize reward to default
			List <ObjectInstance> obs = sp.getObjectsOfClass(GridGame.CLASSAGENT);
			for(ObjectInstance o : obs){
				rewards.put(o.getName(), this.defaultCost(o.getName(), ja));
			}
			
			
			//check for any agents that reached a universal goal location and give them a goal reward if they did
			//List<GroundedProp> upgps = sp.getAllGroundedPropsFor(agentInUniversalGoal);
			List<GroundedProp> upgps = agentInUniversalGoal.getAllGroundedPropsForState(sp);
			for(GroundedProp gp : upgps){
				String agentName = gp.params[0];
				if(gp.isTrue(sp)){
					rewards.put(agentName, uGoalReward);
				}
			}
			
			
			//check for any agents that reached a personal goal location and give them a goal reward if they did
			//List<GroundedProp> ipgps = sp.getAllGroundedPropsFor(agentInPersonalGoal);
			List<GroundedProp> ipgps = agentInPersonalGoal.getAllGroundedPropsForState(sp);
			for(GroundedProp gp : ipgps){
				String agentName = gp.params[0];
				if(gp.isTrue(sp)){
					rewards.put(agentName, this.getPersonalGoalReward(sp, agentName));
				}
			}
			
			List<GroundedProp> rewardProps = agentInRewardGrid.getAllGroundedPropsForState(sp);
			for (GroundedProp gp : rewardProps) {
				String agentName = gp.params[0];
				double reward = sp.getObject(gp.params[1]).getNumericValForAttribute(GridGameExtreme.ATTVALUE);
				if (gp.isTrue(sp)) {
					rewards.put(agentName, reward);
				}
			}
			
			
			return rewards;
			
		}
		
		
		/**
		 * Returns a default cost for an agent assuming the agent didn't transition to a goal state. If noops incur step cost, then this is always the step cost.
		 * If noops do not incur step costs and the agent took a noop, then 0 is returned.
		 * @param aname the name of the agent for which the default reward should be returned.
		 * @param ja the joint action set
		 * @return the default reward; either step cost or 0.
		 */
		protected double defaultCost(String aname, JointAction ja){
			if(this.noopIncursCost){
				return this.stepCost;
			}
			else if(ja.action(aname) == null || ja.action(aname).action.actionName.equals(GridGame.ACTIONNOOP)){
				return 0.;
			}
			return this.stepCost;
		}
		
		
		/**
		 * Returns the personal goal rewards. If a single common personal goal reward was set then that is returned. If different personal goal rewards were defined for each
		 * player number, then that is queried and returned instead.
		 * @param s the state in which the agent player numbers are defined
		 * @param agentName the agent name for which the person goal reward is to be returned
		 * @return the personal goal reward for the specified agent.
		 */
		protected double getPersonalGoalReward(State s, String agentName){
			if(this.personalGoalRewards == null){
				return this.pGoalReward;
			}
			
			int pn = s.getObject(agentName).getIntValForAttribute(GridGame.ATTPN);
			return this.personalGoalRewards.get(pn);
			
		}
		
		

	}
	static class AgentInReward extends PropositionalFunction{

		
		/**
		 * Initializes with the given name and domain and is set to evaluate on agent objects
		 * @param name the name of the propositional function
		 * @param domain the domain for this propositional function
		 */
		public AgentInReward(String name, Domain domain) {
			super(name, domain, new String[]{GridGame.CLASSAGENT, GridGameExtreme.CLASSREWARD});
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance agent = s.getObject(params[0]);
			int ax = agent.getIntValForAttribute(GridGame.ATTX);
			int ay = agent.getIntValForAttribute(GridGame.ATTY);
			
			ObjectInstance reward = s.getObject(params[1]);
			int rx = reward.getIntValForAttribute(GridGame.ATTX);
			int ry = reward.getIntValForAttribute(GridGame.ATTY);
			
			return (ax == rx && ay == ry);
		}
	}

}
