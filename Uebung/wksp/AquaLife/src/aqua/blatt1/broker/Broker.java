package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.Token;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

public class Broker {

	private static final int THREAD_COUNT = 5;
	private Endpoint endpoint;
	private volatile ClientCollection<InetSocketAddress> clients;
	private ExecutorService threadPool;
	private ReadWriteLock clientlock = new ReentrantReadWriteLock();
	Thread stopthread;
	protected volatile boolean stoprequestFlag = false;
	protected volatile Map<String,InetSocketAddress> nameResolutionTable;

	public Broker(int port) {
		endpoint = new Endpoint(port);
		clients = new ClientCollection<InetSocketAddress>();
		threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
		stopthread = new Thread(new StopDialog(this));
		nameResolutionTable = new HashMap<String,InetSocketAddress>();
	}

	public void listen() {
		System.out.println("Start listening...");
		stopthread.start();
		while (!stoprequestFlag) {
			Message message = endpoint.blockingReceive();
			if (message.getPayload() instanceof PoisonPill)
				break;
			threadPool.execute(new BrokerTask(message));
		}
		threadPool.shutdown();
	}

	private class BrokerTask implements Runnable {

		Message message;

		public BrokerTask(Message m) {
			message = m;
		}

		@Override
		public void run() {
			decode(message);
		}

		private void decode(Message message) {
			Object input = message.getPayload();
			switch (input.getClass().getSimpleName()) {
			case "RegisterRequest":
				register(message);
				break;
			case "DeregisterRequest":
				deregister(message);
				break;
			case "HandoffRequest":
				handoff(message);
				break;
			case "NameResolutionRequest":
				performNameResolution(message);
				break;
			default:
				System.out.println("ERROR beim Broker empfangen");
			}
		}

		private void register(Message message) {
			System.out.println("Register");
			InetSocketAddress sender = message.getSender();
			
			clientlock.writeLock().lock();
			String id = "tank" + clients.size();
			
			
			//Send Token to first client
			boolean send_token = (clients.size() == 0);
			
			clients.add(id, sender);
			clientlock.writeLock().unlock();
			
			clientlock.readLock().lock();
			
			updateNeighbor(sender);
			updateNeighbor(clients.getLeftNeighorOf(clients.indexOf(sender)));
			updateNeighbor(clients.getRightNeighorOf(clients.indexOf(sender)));
			
			if(send_token)
				endpoint.send(sender,new Token());
			
			clientlock.readLock().unlock();
			
			synchronized (nameResolutionTable) {
				nameResolutionTable.put(id, sender);
			}
			
			endpoint.send(sender, new RegisterResponse(id));
		}

		private void deregister(Message message) {
			System.out.println("Deregister");
			InetSocketAddress leftN, rightN;

			
			clientlock.writeLock().lock();

			DeregisterRequest request = (DeregisterRequest) message.getPayload();
			int id = clients.indexOf(request.getId());

			leftN = clients.getLeftNeighorOf(id);
			rightN = clients.getRightNeighorOf(id);

			clients.remove(id);

			if (clients.size() > 0) {
				updateNeighbor(leftN);
				updateNeighbor(rightN);
			}

			clientlock.writeLock().unlock();
			
			synchronized (nameResolutionTable) {
				nameResolutionTable.remove("tank" + id);
			}
			
		}

		private void handoff(Message message) {
			System.out.println("Handoff");
			HandoffRequest request = (HandoffRequest) message.getPayload();
			FishModel fish = request.getFish();
			clientlock.readLock().lock();
			int id = clients.indexOf(message.getSender());
			InetSocketAddress target;
			if (fish.getDirection() == Direction.LEFT) {
				target = clients.getLeftNeighorOf(id);
			} else {
				target = clients.getRightNeighorOf(id);
			}
			clientlock.readLock().unlock();
			endpoint.send(target, request);
		}

		private void updateNeighbor(InetSocketAddress client) {
			updateNeighbor(clients.indexOf(client));
		}

		private void updateNeighbor(int id) {
			endpoint.send(clients.getClient(id),
					new NeighborUpdate(clients.getLeftNeighorOf(id), clients.getRightNeighorOf(id)));
		}

	}

	private void performNameResolution(Message message){
		NameResolutionRequest nameResRequ = (NameResolutionRequest) message.getPayload();
		InetSocketAddress tankAdress = nameResolutionTable.get(nameResRequ.getTankID());
		endpoint.send(message.getSender(), new NameResolutionResponse(nameResRequ,tankAdress));
	}
	
	
	public static void main(String[] args) {
		Broker broker = new Broker(4711);
		broker.listen();

	}
}
