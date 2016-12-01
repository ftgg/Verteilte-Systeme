package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NameResolutionResponse implements Serializable {
	private String requestID;
	private InetSocketAddress address;
	
	public NameResolutionResponse(NameResolutionRequest nrr, InetSocketAddress address){
		requestID = nrr.getRequestID();
		this.address = address;
	}
	
	public NameResolutionResponse(String requestID, InetSocketAddress address){
		this.requestID = requestID;
		this.address = address;
	}
	
	public String getRequestID(){
		return requestID;
	}
	
	public InetSocketAddress getAddress(){
		return address;
	}
	
}
