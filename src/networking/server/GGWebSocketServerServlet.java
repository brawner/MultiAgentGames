package networking.server;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class GGWebSocketServerServlet extends WebSocketServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9064300930575111881L;

	@Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(GGWebSocketServer.class);
    }

}
