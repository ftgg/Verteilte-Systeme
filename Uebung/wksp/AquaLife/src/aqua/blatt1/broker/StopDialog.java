package aqua.blatt1.broker;

import javax.swing.JOptionPane;

import aqua.blatt2.broker.Poisoner;

public class StopDialog implements Runnable {

	private Broker broker;

	public StopDialog(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void run() {
		JOptionPane.showMessageDialog(null, "press OK button to stop server");
		System.out.println("STOP!!");
		broker.stoprequestFlag = true;
		new Poisoner().sendPoison();
	}

}
