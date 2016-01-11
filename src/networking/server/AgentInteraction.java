package networking.server;

public class AgentInteraction {
	
	
	String uniqueClientId;
	String uniqueGameId;
	String resultDirectory;
	String agentName;
	
	public AgentInteraction(String uniqueClientId, String uniqueGameId, 
			String resultDirectory, String agentName){
		
		this.uniqueClientId = uniqueClientId;
		this.uniqueGameId = uniqueGameId;
		this.resultDirectory = resultDirectory;
		this.agentName = agentName;
		
	}
	
	

}
