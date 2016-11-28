package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import aqua.blatt1.common.FishModel;

public class ToggleController implements ActionListener {
	private final TankModel tankModel;
	
	ToggleController(TankModel tm){
		tankModel = tm;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		for (FishModel f : tankModel.fishies){
			tankModel.locateFishGlobally(f);
		}
		
		
	}

}
