package Algorithm;

import java.util.List;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args){
    	List<Vertex> vertexs = new ArrayList<Vertex>();
        Vertex a = new Vertex("A", 0);
        Vertex b = new Vertex("B");
        Vertex c = new Vertex("C");
        Vertex d = new Vertex("D");
//        Vertex e = new Vertex("E");
//        Vertex f = new Vertex("F");
        vertexs.add(a);
        vertexs.add(b);
        vertexs.add(c);
        vertexs.add(d);
//        vertexs.add(e);
//        vertexs.add(f);
        int[][] edges = {
                {Integer.MAX_VALUE, 1, Integer.MAX_VALUE, Integer.MAX_VALUE},
                {1, Integer.MAX_VALUE, 100, 2},
                {Integer.MAX_VALUE, 100, Integer.MAX_VALUE,3},
                {Integer.MAX_VALUE, 2, 3, Integer.MAX_VALUE}
        
        };
        Graph graph = new Graph(vertexs, edges);
        graph.search();
        graph.printGraph();
        a = new Vertex("169.254.229.222:6666", 0);
        b = new Vertex("169.254.229.222:6669");
        c = new Vertex("169.254.229.222:6668");
        d = new Vertex("169.254.229.222:6667");
        vertexs.clear();
        vertexs.add(a);
        vertexs.add(b);
        vertexs.add(c);
        vertexs.add(d);
        int[][] edges2 = {
                {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, 3, 2},
                {Integer.MAX_VALUE, 3, Integer.MAX_VALUE, 100},
                {1, 2, 100, Integer.MAX_VALUE}
        
        };
        graph = new Graph(vertexs, edges2);
        graph.search();
        graph.printGraph();
    }
}