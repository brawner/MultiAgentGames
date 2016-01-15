package networking.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import networking.common.GridGameExperimentToken;
import networking.common.GridGameExtreme;
import networking.common.TokenCastException;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.agents.RandomSGAgent;
import burlap.behavior.stochasticgames.agents.madp.MultiAgentDPPlanningAgent;
import burlap.behavior.stochasticgames.agents.naiveq.SGNaiveQLAgent;
import burlap.behavior.stochasticgames.agents.normlearning.ForeverNormLearningAgent;
import burlap.behavior.stochasticgames.agents.normlearning.NormLearningAgent;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.CentralizedDomainGenerator;
import burlap.behavior.stochasticgames.auxiliary.jointmdp.TotalWelfare;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MaxQ;
import burlap.behavior.stochasticgames.madynamicprogramming.dpplanners.MAValueIteration;
import burlap.behavior.stochasticgames.madynamicprogramming.policies.EGreedyMaxWellfare;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGAgent;
import burlap.oomdp.stochasticgames.SGAgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import examples.GridGameNormRF2;

/**
 * The GridGameConfiguration class tracks how a game is to be configured. Instead of configuring a world, the configuration
 * keeps track of the base world, the different agent types and any other information that needs to be tracked for a game.
 * 
 * @author brawner
 *
 */
public class ExperimentConfiguration {
	

	/**
	 * The base type of world for this configuration.
	 */
	private final String uniqueGameID;
	
	/**
	 * The game scores that are tracked through an games run. This should be modified at the end of each run with how reward maps to overall score
	 */
	private final Map<String, Double> scores;
	
	private MatchConfiguration currentMatch;
	
	private List<MatchConfiguration> matchConfigurations;
	
	//private String worldId;
	
	public ExperimentConfiguration() {
		this.uniqueGameID = generateUniqueID();
		this.scores = Collections.synchronizedMap(new HashMap<String, Double>());
		this.matchConfigurations = new ArrayList<MatchConfiguration>();
	}
	
	private String generateUniqueID() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		Random rand = new Random();
		String randVal = Integer.toString(rand.nextInt(Integer.MAX_VALUE));
		
		return dateStr+"_"+randVal;
	}
	/**
	 * Get the score for a specific agent.;
	 * @param agentName
	 * @return
	 */
	public Double getScore(String agentName) {
		synchronized(this.scores) {
			return this.scores.get(agentName);
		}
	}
	
	/** 
	 * Add the updates to the current scores.
	 * @param updates
	 */
	public void appendScores(Map<String, Double> updates) {
		synchronized(this.scores) {
			for (Map.Entry<String, Double> entry : updates.entrySet()) {
				String agentName = entry.getKey();
				Double current = this.scores.get(agentName);
				
				Double update = entry.getValue();
				if (current != null) {
					update += current;
				} 
				this.scores.put(agentName, update);
					
			}
		}
	}
	
	public String getUniqueGameId() {
		
		return uniqueGameID;
	}

	public MatchConfiguration getCurrentMatch() {
		return this.currentMatch;
	}
	
	public List<MatchConfiguration> getAllMatches() {
		return new ArrayList<MatchConfiguration>(this.matchConfigurations);
	}
	
	public int getNumMatches() {
		return this.matchConfigurations.size();
	}
	
	public void advanceToNextMatch() {
		if (this.hasNextMatch()) {
			this.currentMatch = this.matchConfigurations.get(0);
			this.matchConfigurations.remove(0);
		}
	}
	
	public boolean hasNextMatch() {
		return !this.matchConfigurations.isEmpty();
	}
	
	public void addMatchConfig(MatchConfiguration matchConfig) {
		if (this.currentMatch == null) {
			this.currentMatch = matchConfig;
		} else {
			this.matchConfigurations.add(matchConfig);
		}
	}
	
	public boolean isFullyConfigured() {
		for (MatchConfiguration match : this.matchConfigurations) {
			if (!match.isFullyConfigured()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isClosed() {
		for (MatchConfiguration match : this.matchConfigurations) {
			if (!match.isClosed()) {
				return false;
			}
		}
		return true;
	}

	
	public static ExperimentConfiguration createConfigurationFromExperimentStr(
			String yamlString, GridGameServerCollections collections) 
	{
		GridGameExperimentToken token = GridGameExperimentToken.tokenFromJSONString(yamlString);
		List<GridGameExperimentToken> matches;
		try {
			matches = token.getTokenList(GridGameExperimentToken.MATCHES);
		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}
		
		ExperimentConfiguration expConfiguration = new ExperimentConfiguration();
		for (GridGameExperimentToken match : matches) {
			MatchConfiguration matchConfig = MatchConfiguration.getConfigurationFromToken(match, collections);
			expConfiguration.addMatchConfig(matchConfig);
		}
		
		return expConfiguration;
		
		
	}
	

}
