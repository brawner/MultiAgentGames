package networking.server;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class GGWebSocketServerServlet extends WebSocketServlet{

	@Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(GGWebSocketServer.class);
    }

}
