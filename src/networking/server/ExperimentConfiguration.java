package networking.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import networking.common.GridGameExperimentToken;
import networking.common.TokenCastException;
import burlap.mdp.stochasticgames.agent.SGAgentGenerator;

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
	
	private final String activeGameID;
	
	private final String experimentType;
	
	private int matchNumber;
	/**
	 * The game scores that are tracked through an games run. This should be modified at the end of each run with how reward maps to overall score
	 */
	private final Map<String, Double> scores;
	
	private MatchConfiguration currentMatch;
	
	private List<MatchConfiguration> matchConfigurations;
	
	//private String worldId;
	
	public ExperimentConfiguration(String experimentType, String activeGameID) {
		this.uniqueGameID = generateUniqueID();
		this.scores = Collections.synchronizedMap(new HashMap<String, Double>());
		this.matchConfigurations = new ArrayList<MatchConfiguration>();
		this.experimentType = experimentType;
		this.activeGameID = activeGameID;
		this.matchNumber = 0;
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
	
	public Set<String> getAttachedAgentNames() {
		Set<String> agentNames = new HashSet<String>();
		for (MatchConfiguration config : this.matchConfigurations) {
			for (GameHandler handler : config.getHandlerLookup().values()) {
				agentNames.add(handler.getNetworkAgent().agentName());
			}
		}
		
		return agentNames;
	}
	
	public int getNumMatches() {
		return this.matchConfigurations.size();
	}
	
	public int getCurrentMatchNumber() {
		return this.matchNumber;
	}
	
	public void advanceToNextMatch() {
		if (this.hasNextMatch()) {
			this.currentMatch = this.matchConfigurations.get(0);
			this.matchConfigurations.remove(0);
			this.matchNumber++;
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
		if (!this.currentMatch.isFullyConfigured()) {
			return false;
		}
		
		for (MatchConfiguration match : this.matchConfigurations) {
			if (!match.isFullyConfigured()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isClosed() {
		if (!this.currentMatch.isClosed()) {
			return false;
		}
		for (MatchConfiguration match : this.matchConfigurations) {
			if (!match.isClosed()) {
				return false;
			}
		}
		return true;
	}

	
	public static ExperimentConfiguration createConfigurationFromExperimentStr(String experimentType,
			String yamlString, GridGameServerCollections collections, String paramsDirectory, SGAgentGenerator agentGenerator) 
	{
		GridGameExperimentToken token = GridGameExperimentToken.tokenFromJSONString(yamlString);
		List<GridGameExperimentToken> matches;
		try {
			matches = token.getTokenList(GridGameExperimentToken.MATCHES);
		} catch (TokenCastException e) {
			e.printStackTrace();
			return null;
		}
		
		String activeGameId = collections.getUniqueThreadId();
		ExperimentConfiguration expConfiguration = new ExperimentConfiguration(experimentType, activeGameId);
		for (GridGameExperimentToken match : matches) {
			MatchConfiguration matchConfig = MatchConfiguration.getConfigurationFromToken(match, collections, agentGenerator, paramsDirectory);
			if (matchConfig == null) {
				throw new RuntimeException("Setting up match failed");
			}
			expConfiguration.addMatchConfig(matchConfig);
		}
		
		return expConfiguration;
		
		
	}

	public void setMaxIterations(int i) {
		this.currentMatch.setMaxIterations(i);
		for (MatchConfiguration configuration : this.matchConfigurations) {
			configuration.setMaxIterations(i);
		}
	}

	public String getExperimentType() {
		return this.experimentType;
	}
	
	public void addHandler(String clientId, GameHandler handler) {
		this.currentMatch.addHandler(clientId, handler);
		for (MatchConfiguration match : this.matchConfigurations) {
			match.addHandler(clientId, handler);
		}
	}
	
	public String getActiveGameID() {
		return this.activeGameID;
	}
	

}
