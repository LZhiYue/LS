package utilities;

import java.awt.List;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import Algorithm.*;

public class Router {
	
	public ConcurrentLinkedQueue<Message> messageBuffer;
	public RouterId routerId;
	public LinkedHashMap<RouterId, Map<RouterId, Integer>> routingTable;
	public LinkedHashMap<RouterId, Integer> routingTableVersion;
	public int seq = 0;
	public Graph graph;
	public ArrayList<Vertex> vertexs;
	public ArrayList<RouterId> routerIds;
	
	public Router(int serverPort) throws UnknownHostException {
		messageBuffer = new ConcurrentLinkedQueue<>();
		ServerThread serverThread = new ServerThread(serverPort);
		new Thread(serverThread).start();
		MessageHandler messageHandler = new MessageHandler();
		new Thread(messageHandler).start();

		routingTable = new LinkedHashMap<RouterId, Map<RouterId, Integer>>();
		routingTableVersion = new LinkedHashMap<RouterId, Integer>();
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
	
	public void quit() {
		// transfer the new table
		Map<RouterId, Integer> map = routingTable.get(routerId);
		for (RouterId in : map.keySet()) {
			map.put(in, Integer.MAX_VALUE);
		}
		routingTable.put(routerId, map);
		conveyRoutingTableToNeighbors(routerId);
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
		
		vertexs = new ArrayList<>();
		routerIds = new ArrayList<>();
		
		System.out.println("have " + routingTable.keySet().size() + " neighbors");
		
		for (RouterId in : routingTable.keySet()) {
			Vertex vertex = null;
			if (in.toString().equals(routerId.toString())) vertex = new Vertex(in.toString(), 0);
			else vertex = new Vertex(in.toString());
			vertexs.add(vertex);
			routerIds.add(in);
		}
		
		int tableSize = vertexs.size();
		int[][] edges = new int[tableSize][tableSize];
		
		for (int i = 0; i < tableSize; i++) {
			RouterId temp = routerIds.get(i);
			Map<RouterId, Integer> table = routingTable.get(temp);
			for (int j = 0; j < tableSize; j++) {
				if (i == j) {
					edges[i][j] = Integer.MAX_VALUE;
					continue;
				}
				RouterId temp2 = routerIds.get(j);
				if (table.containsKey(temp2)) {
					edges[i][j] = table.get(temp2);
				} else {
					edges[i][j] = Integer.MAX_VALUE;
				}
			}
		}
		
		for (int i = 0; i < tableSize; i++) {
			for (int j = 0; j < tableSize; j++) {
				System.out.print(edges[i][j] + " ");
			}
			System.out.println();
		}
		for (int i = 0; i < vertexs.size(); i++) {
			System.out.print(vertexs.get(i).getName() + " ");
		}
		System.out.println();
		
		graph = new Graph(vertexs, edges);
		graph.search();
		System.out.println("calculate ok");
		graph.printGraph();
	}
	
	public void updateNeighbor(Message m) {
		Map<RouterId, Integer> map = routingTable.get(routerId);
		Integer weight = m.getTable().get(routerId);
		Integer oldWeight = map.get(m.getRouterId());
		if (!weight.equals(oldWeight)) {
			map.put(m.getRouterId(), weight);
			routingTable.put(routerId, map);
			conveyRoutingTableToNeighbors(routerId);
		}
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
			String msg = m.getMessage();
			String[] temps = msg.split(":");
			//System.out.println("message " + msg);
			try {
				RouterId dst = new RouterId(InetAddress.getByName(temps[0]), Integer.valueOf(temps[1]));
				if (dst.toString().equals(routerId.toString())) {
					System.out.println("my message");
					// msg reach the destination
					System.out.println("Message from " + m.getRouterId() + " : " + temps[2]);
				} else {
					// transfer to the dst
					System.out.println("transfer");
					sendMessage(temps[0], Integer.valueOf(temps[1]), temps[2], m.getRouterId());
				}
			} catch (Exception e)  {
				e.printStackTrace();
			}
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
				Set<RouterId> neighors = routingTable.get(routerId).keySet();
				if (neighors.contains(otherRouterId)) {
					updateNeighbor(m);
				}
				
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
		addNeighbor(ip, port, weight);
	}

	public void sendMessage(String ip, int port, String message, RouterId tableOwner) {
		try {
			RouterId dst = new RouterId(InetAddress.getByName(ip), port);
			if (routingTableVersion.containsKey(dst)) {
				int index = routerIds.indexOf(dst);
				if (graph.vertexs.get(index).getPath() != Integer.MAX_VALUE) {
					// can reach the host
					String nextHost = graph.getNext().get(index).getName();
					System.out.println(nextHost);
					
					String[] temps = nextHost.split(":");
					String _ip = temps[0];
					int _port = Integer.valueOf(temps[1]);
					String _message = ip + ":" + Integer.toString(port) + ":" + message;
					
					send(_ip, _port, _message, 0, tableOwner);
				} else {
					System.out.println("Can not reach");
				}
			} else {
				System.out.println("Destination not founded");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendTable(String ip, int port, RouterId tableOwner) {
		send(ip, port, "", 1, tableOwner);
	}
	
	public void test() {
		try {
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6667), 3);
			
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6668), 3);
			/*
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			routingTableVersion.put(new RouterId(InetAddress.getLocalHost(), 6666), 3);
			*/
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
			String s = null;
			while ((s = reader.readLine()) != null) {
				String[] temps = s.split(" ");
				if (temps[0].equals("-c")) {
					router.connect(temps[1], Integer.valueOf(temps[2]), Integer.valueOf(temps[3]));
				} else if (temps[0].equals("-f")) {
					//router.sendTable(temps[1], Integer.valueOf(temps[2]));
					router.conveyRoutingTableToNeighbors(router.routerId);
					break;
				} else if (temps[0].equals("-t")) {
					router.test();
				} else {
					System.out.println("Invalid command");
				}
			}
			System.out.println("Send a message : -s ip port message");
			System.out.println("Quit : -q");
			while ((s = reader.readLine()) != null) {
				String[] temps = s.split(" ");
				if (temps[0].equals("-s")) {
					router.sendMessage(temps[1], Integer.valueOf(temps[2]), temps[3], router.routerId);
				} else if (temps[0].equals("-q")) {
					router.quit();
					break;
				} else {
					System.out.println("Invalid Command");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}