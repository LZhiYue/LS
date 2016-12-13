package Algorithm;

import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Queue;

public class Graph {
    public List<Vertex> vertexs;
    private int[][] edges;
    private Queue<Vertex> unVisited;
    
    private List<Vertex> pre;
    private List<Vertex> next;

    public Graph(List<Vertex> vertexs, int[][] edges) {
        this.vertexs = vertexs;
        this.edges = edges;
        initUnVisited();
        pre = new ArrayList<Vertex>();
        next = new ArrayList<Vertex>();
        for (int i = 0; i < vertexs.size(); i++) {
        	pre.add(vertexs.get(i));
        }
        next.add(vertexs.get(0));
    }
    
    public void search(){
        while(!unVisited.isEmpty()){
            Vertex vertex = unVisited.element();
            vertex.setMarked(true);    
            List<Vertex> neighbors = getNeighbors(vertex);    
            updatesDistance(vertex, neighbors);        
            pop();
        }
       for (int i = 1; i < vertexs.size(); i++) {
    	   Vertex temp = pre.get(i);
    	   int index = vertexs.indexOf(temp);
    	   if (index == 0) {
    		   next.add(vertexs.get(i));
    		   continue;
    	   }
    	   while (pre.get(index).getPath() != 0 && !temp.equals(pre.get(index))) {
    		   temp = pre.get(index);
    		   index = vertexs.indexOf(temp);
    	   }
    	   next.add(temp);
       }
    }
    
    private void updatesDistance(Vertex vertex, List<Vertex> neighbors){
        for(Vertex neighbor: neighbors){
            updateDistance(vertex, neighbor);
        }
    }
    
    private void updateDistance(Vertex vertex, Vertex neighbor){
    	if (vertex.getPath() >= Integer.MAX_VALUE || getDistance(vertex, neighbor) >= Integer.MAX_VALUE)
    		return;
        int distance = getDistance(vertex, neighbor) + vertex.getPath();
        if(distance < neighbor.getPath()){
        	int index = vertexs.indexOf(neighbor);
        	pre.remove(index);
        	pre.add(index, vertex);
            neighbor.setPath(distance);
        }
    }

    private void initUnVisited() {
    	unVisited = new PriorityQueue<Vertex>();
        for (Vertex v : vertexs) {
            unVisited.add(v);
        }
    }

    private void pop() {
        unVisited.poll();
    }
    
    private int getDistance(Vertex source, Vertex destination) {
        int sourceIndex = vertexs.indexOf(source);
        int destIndex = vertexs.indexOf(destination);
        return edges[sourceIndex][destIndex];
    }

    private List<Vertex> getNeighbors(Vertex v) {
        List<Vertex> neighbors = new ArrayList<Vertex>();
        int position = vertexs.indexOf(v);
        Vertex neighbor = null;
        int distance;
        for (int i = 0; i < vertexs.size(); i++) {
            if (i == position) {
                //顶点本身，跳过
                continue;
            }
            distance = edges[position][i];
            if (distance < Integer.MAX_VALUE) {
                //是邻居(有路径可达)
                neighbor = getVertex(i);
                if (!neighbor.isMarked()) {
                    //如果邻居没有访问过，则加入list;
                    neighbors.add(neighbor);
                }
            }
        }
        return neighbors;
    }

    private Vertex getVertex(int index) {
        return vertexs.get(index);
    }

    public void printGraph() {
        int verNums = vertexs.size();
        for (int i = 0; i < verNums; i++)
        	System.out.print(vertexs.get(i).getPath() + " ");
        /*System.out.println();
        for (int i = 0; i < verNums; i++)
        	System.out.print(pre.get(i).getName() + " ");*/
        System.out.println();
        for (int i = 0; i < next.size(); i++)
        	System.out.print(next.get(i).getName() + " ");
        System.out.println();
    }
    //获取到达每个节点的权值
    public int[] getValues() {
    	int[] values = new int[vertexs.size()];
    	for (int i = 0; i < vertexs.size(); i++)
    		values[i] = vertexs.get(i).getPath();
    	return values;
    }
    //获取到达每个节点的下一个节点
    public List<Vertex> getNext() {
    	return this.next;
    }
}