package edu.isistan.seas.proxy.jobstealing.condition;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;

public class NoJobsCondition implements StealingCondition {

	@Override
	public boolean canSteal(Device stealer, Device victim, StealerProxy proxy) {
		return stealer.getWaitingJobs() == 0;
	}

}
