package Algorithm;

public class Vertex implements Comparable<Vertex>{
	private  static int count = 0;
	
    private String name;
    private int path;
    private boolean isMarked;
    private int id;
    
    public Vertex(String name){
        this.name = name;
        this.path = Integer.MAX_VALUE;
        this.setMarked(false);
        this.id = count++;
    }
    
    public Vertex(String name, int path){
        this.name = name;
        this.path = path;
        this.setMarked(false);
        this.id = count++;
    }
    
    public int getId() {
    	return this.id;
    }
    
    public String getName() {
    	return this.name;
    }
    
    @Override
    public int compareTo(Vertex o) {
        return o.path > path?-1:1;
    }
    
    public void setMarked(boolean isMarked) {
    	this.isMarked = isMarked;
    }
    
    public boolean isMarked() {
    	return this.isMarked;
    }
    
    public int getPath() {
    	return this.path;
    }
    
    public void setPath(int dis) {
    	this.path = dis;
    }
}