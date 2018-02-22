package edu.isistan.seas.proxy.jobstealing.condition;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;

public class BatteryDifferenceCondition implements StealingCondition {

	private double difference = 2;
	@Override
	public boolean canSteal(Device stealer, Device victim, StealerProxy proxy) {
		SchedulerProxy schedulerProxy = SchedulerProxy.PROXY;
		return schedulerProxy.getLastReportedSOC(stealer) > difference * schedulerProxy.getLastReportedSOC(victim);
	}
	
	public void setDifference(String s){
		this.difference = Double.parseDouble(s);
	}

}
