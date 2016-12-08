package utilities;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Router {
	
	public ConcurrentLinkedQueue<Message> messageBuffer;
	public RouterId routerId;
	public Map<RouterId, Map<RouterId, Integer>> routingTable;
	public Map<RouterId, Integer> routingTableVersion;
	public int seq = 0;

	public Router(int serverPort) throws UnknownHostException {
		messageBuffer = new ConcurrentLinkedQueue<>();
		ServerThread serverThread = new ServerThread(serverPort);
		new Thread(serverThread).start();
		MessageHandler messageHandler = new MessageHandler();
		new Thread(messageHandler).start();

		routingTable = new HashMap<RouterId, Map<RouterId, Integer>>();
		routingTableVersion = new HashMap<RouterId, Integer>();
		routerId = new RouterId(InetAddress.getLocalHost(), serverPort);
		System.out.println(InetAddress.getLocalHost().getHostAddress());
	}
	
	public void addNeighbor(String ip, int port, int weight) {
		if (!routingTable.containsKey(routerId)) {
			Map<RouterId, Integer> map = new HashMap<RouterId, Integer>();
			try {
				map.put(new RouterId(InetAddress.getByName(ip), port), weight);
			    routingTable.put(routerId, map);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Map<RouterId, Integer> map = routingTable.get(routerId);
			try {
				map.put(new RouterId(InetAddress.getByName(ip), port), weight);
				routingTable.put(routerId, map);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		routingTableVersion.put(routerId, this.seq);
	}
	
	public void conveyRoutingTableToNeighbors(RouterId tableOwner) {
		Map<RouterId, Integer> map = routingTable.get(routerId);
		for (RouterId in : map.keySet()) {
			// convey the table
			sendTable(in.address.getHostAddress(), in.port, tableOwner);
		}
	}
	
	public void calculate() {
		System.out.println("calculate");
	}

	class ServerThread implements Runnable {
		private int serverPort;
		private ServerSocket server;

		public ServerThread(int serverPort) {
			this.serverPort = serverPort;
		}

		public void run() {
			try {
				server = new ServerSocket(serverPort);
				while (true) {
					Socket socket = server.accept();
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					messageBuffer.add(new Message(socket.getInetAddress(), (RoutingMessage)in.readObject()));
					in.close();
					socket.close();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void handleMessage(Message m) {
		/*
		 * Tag 0 means message
		 * Tag 1 means router table
		 */
		if (m.getTag() == 0) {
			System.out.println(m.getMessage());
		} else if (m.getTag() == 1) {
			RouterId otherRouterId = m.getRouterId();
			System.out.println("Received");
			if (otherRouterId.toString().equals(routerId.toString())) {
				System.out.println("Myself");
			}
			else if (!routingTableVersion.containsKey(otherRouterId) || routingTableVersion.get(otherRouterId) < m.getSeq()) {
				// update routing table
				routingTable.put(otherRouterId, m.getTable());
				routingTableVersion.put(otherRouterId, m.getSeq());
				
				System.out.println("Table");
				System.out.println(otherRouterId);
				calculate();
				conveyRoutingTableToNeighbors(otherRouterId);
			} else {
				System.out.println("exist");
			}
			System.out.println("finish");
		}
	}

	class MessageHandler implements Runnable {
		public void run() {
			while (true) {
				if (!messageBuffer.isEmpty()) {
					handleMessage(messageBuffer.poll());
					try {
						Thread.sleep(10);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public RoutingMessage createRoutingMessage(int tag, String message, RouterId tableOwner) {
		RoutingMessage routingMessage = new RoutingMessage(tableOwner);
		routingMessage.tag = tag;
		if (tag == 0) {
			routingMessage.message = message;
		} else if (tag == 1) {
			if (routerId == tableOwner) {
				routingMessage.seq = this.seq++;
				routingMessage.neighbours = routingTable.get(routerId);
			} else {
				routingMessage.seq = routingTableVersion.get(tableOwner);
				routingMessage.neighbours = routingTable.get(tableOwner);
			}
		}
		return routingMessage;
	}

	protected boolean send(String ip, int port, String message, int tag, RouterId tableOwner) {
		try {
			Socket socket = new Socket(ip, port);
			RoutingMessage sendMessage = createRoutingMessage(tag, message, tableOwner);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(sendMessage);
			out.flush();
			out.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void connect(String ip, int port, int weight) {
		//send(ip, port, "Connect", 0);
		addNeighbor(ip, port, weight);
	}

	public void sendMessage(String ip, int port, String message) {
		//send(ip, port, message, 0, );
	}
	
	public void sendTable(String ip, int port, RouterId tableOwner) {
		send(ip, port, "", 1, tableOwner);
	}
	
	public void test() {
		routingTableVersion = new HashMap<>();
		try {
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			if (routingTableVersion.containsKey(new RouterId(InetAddress.getLocalHost(), 6666))) {
				System.out.println("ok");
			} else {
				System.out.println("bb");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(routingTableVersion.size());
		for (RouterId in : routingTableVersion.keySet()) {
			System.out.println(in + " " + routingTableVersion.get(in));
		}
	}

	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please input port");
			Router router = new Router(Integer.valueOf(reader.readLine()));
			System.out.println("Connect a router : -c ip port weight");
			System.out.println("Finish connect : -f");
			System.out.println("Send a message : -s ip port message");
			String s = null;
			while ((s = reader.readLine()) != null) {
				String[] temps = s.split(" ");
				if (temps[0].equals("-c")) {
					router.connect(temps[1], Integer.valueOf(temps[2]), Integer.valueOf(temps[3]));
				} else if (temps[0].equals("-s")) {
					router.sendMessage(temps[1], Integer.valueOf(temps[2]), temps[3]);
				} else if (temps[0].equals("-f")) {
					//router.sendTable(temps[1], Integer.valueOf(temps[2]));
					router.conveyRoutingTableToNeighbors(router.routerId);
				} else if (temps[0].equals("-t")) {
					router.test();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}