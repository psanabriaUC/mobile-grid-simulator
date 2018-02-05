package edu.isistan.seas.proxy.jobstealing.condition;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;

public interface StealingCondition {

	public boolean canSteal(Device stealer, Device victim, StealerProxy proxy);
}
