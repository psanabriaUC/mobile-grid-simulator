package edu.isistan.seas.node;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class InfiniteBatteryManager implements DefaultBatteryManager {
    private static final int MAX_CHARGE = 1000000;
    private Device device;
    private long startTime;
    private Event lastAddedEvent;
    private DefaultExecutionManager executionManager;
    private long lastMeasurement;

    public InfiniteBatteryManager() {
    }

    @Override
    public void onBeginExecutingJobs() {
    }

    @Override
    public void onStopExecutingJobs() {
    }

    @Override
    public void onNetworkEnergyConsumption(double decreasedBatteryPercentage) {

    }

    @Override
    public void onBatteryEvent(int level) {
        if (level <= 0) {
            this.lastMeasurement = Simulation.getTime();
            this.device.onBatteryDepletion();
        }
    }

    @Override
    public void onUserActivityEvent(boolean flag) {

    }

    @Override
    public int getCurrentBattery() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getBatteryCapacityInJoules() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getEstimatedUptime() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void startWorking() {
        this.lastMeasurement = Simulation.getTime();
        this.startTime = Simulation.getTime();
        this.lastAddedEvent = Event.createEvent(Event.NO_SOURCE, this.lastMeasurement, this.device.getId(),
                Device.EVENT_TYPE_BATTERY_UPDATE, MAX_CHARGE);
        Simulation.addEvent(this.lastAddedEvent);
        Logger.logEntity(device, "Device started");
    }

    @Override
    public void shutdown() {
        Simulation.removeEvent(this.lastAddedEvent);
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getInitialSOC() {
        return MAX_CHARGE;
    }

    @Override
    public double getCurrentSOC() {
        return MAX_CHARGE;
    }

    @Override
    public void addProfileData(int prof, ProfileData dat) {

    }

    @Override
    public DefaultExecutionManager getSEASExecutionManager() {
        return executionManager;
    }

    @Override
    public void setSEASExecutionManager(DefaultExecutionManager seasEM) {
        this.executionManager = seasEM;
    }

    @Override
    public Device getDevice() {
        return this.device;
    }

    @Override
    public void setDevice(Device device) {
        this.device = device;
    }
}
