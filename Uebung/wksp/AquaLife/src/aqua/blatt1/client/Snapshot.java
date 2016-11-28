package aqua.blatt1.client;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Snapshot implements Serializable{
	private int FishCount;

	Snapshot(int FishCount) {
		this.FishCount = FishCount;
	}

	Snapshot() {
		this(0);
	}

	Snapshot(Snapshot sn) {
		this(sn.getValue());
	}

	public int getValue() {
		return FishCount;
	}

	public Snapshot combine(Snapshot s) {
		FishCount += s.getValue();
		return this;
	}
}
