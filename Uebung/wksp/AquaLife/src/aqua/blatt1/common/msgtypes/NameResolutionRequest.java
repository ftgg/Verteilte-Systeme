package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NameResolutionRequest implements Serializable {

	private String requestID;
	private String tankID;
	

	public NameResolutionRequest(String id, String tid){
		requestID = id;
		tankID = tid;
	}

	public String getRequestID() {
		return requestID;
	}

	public String getTankID() {
		return tankID;
	}
}
 