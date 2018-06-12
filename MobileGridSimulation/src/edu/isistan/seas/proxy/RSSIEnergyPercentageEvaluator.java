package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.node.DefaultBatteryManager;

public class RSSIEnergyPercentageEvaluator extends RSSIEvaluator {

	@Override
	public double eval(DataAssignment dataAssignment) {
		return (double) dataAssignment.getDevice().getWifiRSSI() + (double)(dataAssignment.getDevice().getBatteryLevel() /
				DefaultBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) -
				((dataAssignment.getMbToBeReceived()+dataAssignment.getMbToBeSend()) * 1024 * getRSSIEnergyPercentageConsumption(dataAssignment.getDevice()));
	}
}
