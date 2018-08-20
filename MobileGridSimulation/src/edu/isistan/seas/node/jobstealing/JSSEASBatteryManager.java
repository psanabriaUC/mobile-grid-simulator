package edu.isistan.seas.node.jobstealing;

import edu.isistan.seas.node.DefaultFiniteBatteryManager;

public class JSSEASBatteryManager extends DefaultFiniteBatteryManager {

    public JSSEASBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules) {
        super(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public void startWorking() {
        super.startWorking();
        //((StealerProxy)SchedulerProxy.PROXY).steal(this.getDevice());
    }


}
