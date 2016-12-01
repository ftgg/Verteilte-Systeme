package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.SnapshotCollector;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.Token;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(InetSocketAddress neighbor, FishModel fish) {
			endpoint.send(neighbor, new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress neighbor) {
			endpoint.send(neighbor, new Token());
		}

		public void sendMarker(InetSocketAddress neighbor) {
			endpoint.send(neighbor, new SnapshotMarker());
		}

		public void sendCollector(InetSocketAddress neighbor, SnapshotCollector snc) {
			endpoint.send(neighbor, snc);
		}
		
		public void sendLocationRequest(InetSocketAddress neighbor, String fish){
			endpoint.send(neighbor, new LocationRequest(fish));
		}
		
		public void sendNameResolutionRequest(NameResolutionRequest nameResRequ){
			endpoint.send(broker, nameResRequ);
		}
		
		public void sendLocationUpdate(InetSocketAddress target, String fishID){
			endpoint.send(target, new LocationUpdate(fishID));
		}
		
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());
				else if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
				else if (msg.getPayload() instanceof NeighborUpdate) {
					NeighborUpdate nu = (NeighborUpdate) msg.getPayload();
					tankModel.setLeftNeighbor(nu.getLeftNeighbor());
					tankModel.setRightNeighbor(nu.getRightNeighbor());
				} else if (msg.getPayload() instanceof Token)
					tankModel.receiveToken();
				else if (msg.getPayload() instanceof SnapshotMarker)
					tankModel.receiveMarker(msg.getSender());
				else if (msg.getPayload() instanceof SnapshotCollector)
					tankModel.receiveCollector((SnapshotCollector) msg.getPayload());
				else if(msg.getPayload() instanceof LocationRequest)
					tankModel.receiveLocationRequest((LocationRequest) msg.getPayload());
				else if(msg.getPayload() instanceof NameResolutionResponse)
					tankModel.receiveNameResolutionResponse((NameResolutionResponse) msg.getPayload());
				else if(msg.getPayload() instanceof LocationUpdate){
					LocationUpdate lu = (LocationUpdate) msg.getPayload();
					tankModel.receiveLocationUpdate(msg.getSender(), lu.getFishID());
				}

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
