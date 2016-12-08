package utilities;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by Shower on 2016/12/8 0008.
 */
public class RouterId implements Serializable {
    public InetAddress address;
    public int port;


    public RouterId(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public String toString() {
        return address.getHostAddress() + ":" + port;
    }
    
    public int hashCode() {
    	return this.address.hashCode() * this.port;
    }
    
    public boolean equals(Object obj) {
    	RouterId temp = (RouterId)obj;
    	if (temp.toString().equals(this.toString())) {
    		return true;
    	} else {
    		return false;
    	}
    }
}
