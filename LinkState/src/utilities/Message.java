package utilities;

import java.net.*;
import java.util.Map;

public class Message {
	public InetAddress address;
	public RoutingMessage message;

	public Message(InetAddress address, RoutingMessage routingMessage) {
		this.address = address;
		this.message = routingMessage;
	}
	
	public String getMessage() {
		return message.message;
	}
	
	public Map<RouterId, Integer> getTable() {
		return message.neighbours; 
	}
	
	public int getSeq() {
		return message.seq;
	}
	
	public RouterId getRouterId() {
		return message.routerId;
	}
	
	public int getTag() {
		return message.tag;
	}
}