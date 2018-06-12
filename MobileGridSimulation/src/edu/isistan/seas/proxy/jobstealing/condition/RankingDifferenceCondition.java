package edu.isistan.seas.proxy.jobstealing.condition;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;

public class RankingDifferenceCondition implements StealingCondition {

	private double difference = 2;
	@Override
	public boolean canSteal(Device stealer, Device victim, StealerProxy proxy) {
		DeviceComparator comparator = proxy.getDevComp();
		return comparator.getValue(stealer) > difference*comparator.getValue(victim);
	}
	
	public void setDifference(String s){
		this.difference = Double.parseDouble(s);
	}

}
