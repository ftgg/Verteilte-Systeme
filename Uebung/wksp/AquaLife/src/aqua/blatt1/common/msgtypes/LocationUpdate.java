package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LocationUpdate implements Serializable {
	private String fishID;
	
	public LocationUpdate(String fishID){
		this.fishID = fishID;
	}
	
	public String getFishID(){
		return fishID;
	}
	
}