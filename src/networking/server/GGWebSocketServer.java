package networking.server;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import networking.common.GridGameServerToken;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;


public class GGWebSocketServer extends WebSocketAdapter{
	private static final String WORLD_DIRECTORY = System.getProperty("user.home") + "/grid_games/worlds";
	private static final String OUTPUT_DIRECTORY = System.getProperty("user.home") + "/grid_games/results";
	private GridGameManager server;
	private Session session;
	
	public GGWebSocketServer() {
		this.server = GridGameManager.connect();
	}
	public GGWebSocketServer(String gameDirectory, String outputDirectory) {
		this.server = GridGameManager.connect(gameDirectory, outputDirectory);
	}
	
	@Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);
        this.server.onConnect(sess);
        this.session = sess;
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        if (!message.contains(GameHandler.HEARTBEAT)) {
            System.out.println("Received TEXT message: " + message);        	
        }
        GridGameServerToken token = GridGameServerToken.tokenFromJSONString(message);
        GridGameServerToken response = this.server.onMessage(token);
        try {
        	if (!response.isEmpty()) {
				this.session.getRemote().sendString(response.toJSONString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        this.server.onWebSocketClose(this.session);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
	
	
	public static void main(String[] args) {
		String gameDirectory = WORLD_DIRECTORY;
		String outputDirectoryRoot = OUTPUT_DIRECTORY;
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		String outputDirectory = outputDirectoryRoot + "/" + dateStr;
		File outputDirFile = new File(outputDirectory);
		if (!outputDirFile.mkdirs()) {
			throw new RuntimeException("Could not make the output directory " + outputDirFile.getAbsolutePath());
		}
		
		if (args.length > 1) {
			gameDirectory = args[0];
			outputDirectory = args[1];
		}
		
		GGWebSocketServer ggServer = new GGWebSocketServer(gameDirectory, outputDirectory);
		Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8787);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // Add a websocket to a specific path spec
        ServletHolder holderEvents = new ServletHolder("ws-events", GGWebSocketServerServlet.class);
        
        context.addServlet(holderEvents, "/events/*");

        try
        {
            server.start();
            System.out.println("Server started at: " + server.getURI().toString());
            //server.dump(System.err);
            server.join();
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
	}

	
	

}
