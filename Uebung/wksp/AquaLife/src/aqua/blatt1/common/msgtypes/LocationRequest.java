package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

import aqua.blatt1.common.FishModel;

public class LocationRequest implements Serializable {
	private String fishie;

	public LocationRequest(String fishie){
		this.fishie = fishie;
	}
	
	public String getFish() {
		return fishie;
	}
	
	

}
