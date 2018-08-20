package edu.isistan.seas.node;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;

public interface DefaultBatteryManager extends BatteryManager {
    void addProfileData(int prof, ProfileData dat);

    DefaultExecutionManager getSEASExecutionManager();

    void setSEASExecutionManager(DefaultExecutionManager seasEM);

    Device getDevice();

    void setDevice(Device device);
}
