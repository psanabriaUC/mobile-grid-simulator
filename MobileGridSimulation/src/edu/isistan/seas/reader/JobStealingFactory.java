package edu.isistan.seas.reader;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.mobileGrid.node.NetworkEnergyManager;
import edu.isistan.seas.node.DefaultBatteryManager;
import edu.isistan.seas.node.DefaultExecutionManager;
import edu.isistan.seas.node.DefaultNetworkEnergyManager;
import edu.isistan.seas.node.jobstealing.JSDevice;
import edu.isistan.seas.node.jobstealing.JSSEASBatteryManager;
import edu.isistan.seas.node.jobstealing.JSSEASExecutionManager;

public class JobStealingFactory implements ManagerFactory {

	@Override
	public DefaultBatteryManager createBatteryManager(int prof, int charge,
			long estUptime,long batteryCapacityInJoules) {
		return new JSSEASBatteryManager(prof, charge, estUptime,batteryCapacityInJoules);
	}

	@Override
	public DefaultExecutionManager createExecutionManager() {
		return new JSSEASExecutionManager();
	}

	@Override
	public DefaultNetworkEnergyManager createNetworkEnergyManager(
			boolean enableNetworkExecutionManager, short wifiSignalStrength) {		
		return new DefaultNetworkEnergyManager(enableNetworkExecutionManager,wifiSignalStrength);
	}

	@Override
	public Device createDevice(String name, BatteryManager bt,	ExecutionManager em, NetworkEnergyManager nem) {		
		return new JSDevice(name, bt, em, nem);
	}

}
