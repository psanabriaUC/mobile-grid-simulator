package edu.isistan.seas.node.jobstealing;

import edu.isistan.seas.node.DefaultBatteryManager;

public class JSSEASBatteryManager extends DefaultBatteryManager{

	public JSSEASBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules) {
		super(prof, charge, estUptime, batteryCapacityInJoules);
	}

	@Override
	public void startWorking() {
		super.startWorking();
		//((StealerProxy)SchedulerProxy.PROXY).steal(this.getDevice());
	}
	
	

}
