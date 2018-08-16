package edu.isistan.seas.node;

public class InfiniteBatteryManager extends DefaultBatteryManager {

    /**
     * Builds a standard battery manager with default capabilities to emulate battery discharges when idle, executing
     * CPU intensive jobs, and transferring data over a network.
     *
     * @param prof                    Number of trace data sets to emulate battery discharge under different workloads.
     * @param charge                  Initial state of charge of the device's battery (0 - 1000000).
     * @param estUptime               Estimated time until discharge in milliseconds.
     * @param batteryCapacityInJoules Battery capacity in Joules.
     */
    public InfiniteBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules) {
        super(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public void onNetworkEnergyConsumption(double decreasingPercentageValue) {
        super.onNetworkEnergyConsumption(decreasingPercentageValue);
    }

    @Override
    public void onBatteryEvent(int level) {
        super.onBatteryEvent(level);
    }

    @Override
    public void onUserActivityEvent(boolean screenOn) {
        super.onUserActivityEvent(screenOn);
    }

    @Override
    public void onBeginExecutingJobs() {
        super.onBeginExecutingJobs();
    }

    @Override
    public void onStopExecutingJobs() {
        super.onStopExecutingJobs();
    }

    @Override
    public void startWorking() {
        super.startWorking();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public void addProfileData(int prof, ProfileData dat) {
        super.addProfileData(prof, dat);
    }
}
