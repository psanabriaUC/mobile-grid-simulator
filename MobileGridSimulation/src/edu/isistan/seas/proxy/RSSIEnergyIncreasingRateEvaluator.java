package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.node.DefaultBatteryManager;

public class RSSIEnergyIncreasingRateEvaluator implements DataAssignmentEvaluatorIF {

	@Override
	public double eval(DataAssignment da) {
		return (double) da.getDevice().getWifiRSSI() + (double)(da.getDevice().getBatteryLevel() /
				DefaultBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) -
                ((((da.getMbToBeReceived()+da.getMbToBeSend()) * 1024) / 100) * getRSSIEnergyConsumptionRate(da.getDevice()));
	}

	private double getRSSIEnergyConsumptionRate(Device device) {
		switch (device.getWifiRSSI()) {
			case -50 : return 1;
			case -80 : return 1.21;
			case -85 : return 1.78;
			case -90 : return 6.78;
			default: return 0;
		}		
	}

}
