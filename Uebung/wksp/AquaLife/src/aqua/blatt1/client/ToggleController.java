package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import aqua.blatt1.common.FishModel;

public class ToggleController implements ActionListener {
	private final TankModel tankModel;

	ToggleController(TankModel tm) {
		tankModel = tm;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String fishID = ((JMenuItem) e.getSource()).getText();
		tankModel.locateFishGlobally(fishID);
	}

}
