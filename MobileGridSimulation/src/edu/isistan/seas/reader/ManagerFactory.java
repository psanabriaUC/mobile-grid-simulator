package edu.isistan.seas.reader;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.mobileGrid.node.NetworkEnergyManager;
import edu.isistan.seas.node.DefaultBatteryManager;
import edu.isistan.seas.node.DefaultExecutionManager;
import edu.isistan.seas.node.DefaultNetworkEnergyManager;

public interface ManagerFactory {

	public DefaultBatteryManager createBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules);
	
	public DefaultExecutionManager createExecutionManager();

	public DefaultNetworkEnergyManager createNetworkEnergyManager(boolean enableNetworkExecutionManager, short wifiSignalString);
	
	public Device createDevice(String name, BatteryManager bt, ExecutionManager em,	NetworkEnergyManager nem);

}
