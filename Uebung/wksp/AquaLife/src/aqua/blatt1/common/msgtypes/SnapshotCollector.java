package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

import aqua.blatt1.client.Snapshot;

public class SnapshotCollector implements Serializable {
	private Snapshot sn;
	
	public SnapshotCollector(Snapshot sn){
		this.sn = sn;
	}
	
	public void combine(Snapshot localsnap){
		sn.combine(localsnap);
	}

	public Snapshot getSnapShot() {
		return sn;
	}
	
}
