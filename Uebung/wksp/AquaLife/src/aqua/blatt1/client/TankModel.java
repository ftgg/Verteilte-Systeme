package aqua.blatt1.client;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.SnapshotCollector;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import messaging.Message;

import java.util.Timer;
import java.util.TimerTask;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 7;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	private InetSocketAddress rightNeighbor, leftNeighbor;
	private volatile boolean hasToken = false;
	private Snapshot localSnap;
	private volatile RecordingState tankRecState = RecordingState.IDLE;
	private volatile boolean snapshotinitiator = false;
	private volatile Map<String, InetSocketAddress> homeAgent;

	public synchronized void initiateSnapshot() {
		localSnap = new Snapshot(fishCounter);
		snapshotinitiator = true;
		tankRecState = RecordingState.BOTH;
		forwarder.sendMarker(leftNeighbor);
		forwarder.sendMarker(rightNeighbor);
	}

	public void setRightNeighbor(InetSocketAddress rightNeighbor) {
		this.rightNeighbor = rightNeighbor;
	}

	public void setLeftNeighbor(InetSocketAddress leftNeighbor) {
		this.leftNeighbor = leftNeighbor;
	}

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
		homeAgent = new HashMap<String, InetSocketAddress>();
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);

		if (fish.getDirection() == Direction.LEFT)
			if (tankRecState == RecordingState.BOTH || tankRecState == RecordingState.LEFT)
				localSnap.combine(new Snapshot(1));
			else
				;
		else if (tankRecState == RecordingState.BOTH || tankRecState == RecordingState.RIGHT)
			localSnap.combine(new Snapshot(1));
		
		performNameResolution(fish.getId(),fish.getTankId());
	}

	public synchronized void receiveMarker(InetSocketAddress sender) {
		switch (tankRecState) {
		case RIGHT:
		case LEFT:
			tankRecState = RecordingState.IDLE;
			if (snapshotinitiator) {
				// TODO das passt noch nicht!
				forwarder.sendCollector(leftNeighbor, new SnapshotCollector(localSnap));
			}
			break;

		case IDLE:
			localSnap = new Snapshot(fishCounter);
			forwarder.sendMarker(leftNeighbor);
			forwarder.sendMarker(rightNeighbor);

		case BOTH:
			if (sender.equals(rightNeighbor)) {
				tankRecState = RecordingState.LEFT;
			} else {
				tankRecState = RecordingState.RIGHT;
			}

			break;
		}

	}

	private void debug(String string) {
		System.out.println(string);
	}

	protected synchronized void receiveToken() {
		hasToken = true;
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			public void run() {
				hasToken = false;
				forwarder.sendToken(leftNeighbor);
			}
		};
		timer.schedule(task, 2000);
	}

	protected synchronized boolean hasToken() {
		return hasToken;
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (hasToken) {
					if (fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(leftNeighbor, fish);
					} else {
						forwarder.handOff(rightNeighbor, fish);
					}
				} else {
					fish.reverse();
				}
			}

			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		while(hasToken);
		forwarder.deregister(id);
	}

	// receive Collector in extra thread
	public synchronized void receiveCollector(SnapshotCollector snapshotCollector) {

		while (tankRecState != RecordingState.IDLE)
			;

		if (snapshotinitiator) {
			snapshotinitiator = false;
			handleSnapshot(snapshotCollector.getSnapShot());
		} else {
			snapshotCollector.combine(localSnap);
			localSnap = new Snapshot(0);
			forwarder.sendCollector(leftNeighbor, snapshotCollector);
		}

	}

	private void handleSnapshot(Snapshot snapShot) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(null, "Anzahl Fische: " + snapShot.getValue());
			}
		});
	}

	private void locateFishLocally(String fishID) {
		for (FishModel f : fishies)
			if (f.getId().equals(fishID)) {
				f.toggle();
				break;
			}

	}

	public void receiveLocationRequest(LocationRequest payload) {
		locateFishLocally(payload.getFish());
	}

	public synchronized void locateFishGlobally(String fishID) {	
		InetSocketAddress targetTank = homeAgent.get(fishID);
		if (targetTank == null) {
			locateFishLocally(fishID);
			debug("locale suche");
		} else {
			debug("fisch au�erhalb");
			forwarder.sendLocationRequest(targetTank, fishID);
		}
	}
	
	public synchronized void performNameResolution(String fishID, String tankID) {
		if(homeAgent.containsKey(fishID))
			homeAgent.put(fishID, null);
		else
			forwarder.sendNameResolutionRequest(new NameResolutionRequest(fishID,tankID));
	}
	
	public void receiveNameResolutionResponse(NameResolutionResponse nameResResp) {
		forwarder.sendLocationUpdate(nameResResp.getAddress(), nameResResp.getRequestID());
	}
	
	public synchronized void receiveLocationUpdate(InetSocketAddress newLocation, String fishID){
		homeAgent.put(fishID, newLocation);
	}
	
}