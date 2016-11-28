package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {
	
	private InetSocketAddress left, right;
	
	public NeighborUpdate(InetSocketAddress left,InetSocketAddress right){
		this.left = left;
		this.right = right;
	}

	public InetSocketAddress getLeftNeighbor() {
		return left;
	}

	public InetSocketAddress getRightNeighbor() {
		return right;
	}

}
