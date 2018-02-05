package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.node.Device;

public class RSSIDataEvaluator implements DataAssignmentEvaluatorIF {

	@Override
	public double eval(DataAssignment da) {		
		return (da.getMbToBeReceived()+da.getMbToBeSend())* 1024 * getRSSIEnergyPercentageConsumption(da.getDevice());
	}
	
	private double getRSSIEnergyPercentageConsumption(Device d) {
		switch(d.getWifiRSSI()){
			case -50: return (double)((double)(0.0018648 * 100) / d.getTotalBatteryCapacityInJoules());
			case -80: return (double)((double)(0.0022644 * 100) / d.getTotalBatteryCapacityInJoules());
			case -85: return (double)((double)(0.0033 * 100) / d.getTotalBatteryCapacityInJoules());
			case -90: return (double)((double)(0.012654 * 100) / d.getTotalBatteryCapacityInJoules());
			default: return 100;//if signal is not in the above contemplated values then return an energy consumption of 100%
		}	
	}

}
