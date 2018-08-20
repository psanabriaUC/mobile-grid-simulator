package edu.isistan.seas.reader;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.mobileGrid.node.NetworkEnergyManager;
import edu.isistan.seas.node.*;

public class DefaultManagerFactory implements ManagerFactory {

    @Override
    public DefaultBatteryManager createBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules, boolean isInfinite) {
        if (isInfinite)
            return new InfiniteBatteryManager();
        else
            return new DefaultFiniteBatteryManager(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public DefaultExecutionManager createExecutionManager(boolean isInfinite) {
        if (isInfinite)
            return new InfiniteExecutionManager();
        else
            return new DefaultExecutionManager();
    }

    @Override
    public DefaultNetworkEnergyManager createNetworkEnergyManager(boolean enableNetworkExecutionManager, short wifiSignalString) {
        return new DefaultNetworkEnergyManager(enableNetworkExecutionManager, wifiSignalString);
    }

    @Override
    public Device createDevice(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem) {
        return new Device(name, bt, em, nem);
    }


}
