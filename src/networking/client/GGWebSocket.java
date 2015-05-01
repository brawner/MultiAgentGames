package networking.client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import networking.common.GridGameServerToken;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class GGWebSocket extends WebSocketAdapter {
	private final List<GGWebSocketListener> listeners;
	private final ReadWriteLock lock;
	private Session session;
	public GGWebSocket() {
		this.listeners = new ArrayList<GGWebSocketListener>();
		this.lock = new ReentrantReadWriteLock();
	}
	@Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);
        this.session = sess;
    }
    
    @Override
    public void onWebSocketText(String message)
    {
    	super.onWebSocketText(message);
        System.out.println("Received TEXT message: " + message);
        GridGameServerToken token = GridGameServerToken.tokenFromJSONString(message);
        
        
        
        try {
        	this.lock.readLock().lock();
        	List<GGWebSocketListener> listeners = new ArrayList<GGWebSocketListener>(this.listeners);
        	this.lock.readLock().unlock();
        	for (GGWebSocketListener listener : listeners) {
				GridGameServerToken response = listener.onMessage(token);
				if (response != null && !response.isEmpty()) {
					this.session.getRemote().sendString(response.toJSONString());
				}
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
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
    
    public void addListener(GGWebSocketListener listener) {
    	this.lock.writeLock().lock();
    	this.listeners.add(listener);
    	this.lock.writeLock().unlock();
    }

}
