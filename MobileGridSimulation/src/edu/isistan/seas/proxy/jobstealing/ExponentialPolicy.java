package edu.isistan.seas.proxy.jobstealing;

import java.util.HashMap;
import java.util.Map;

import edu.isistan.mobileGrid.node.Device;

public class ExponentialPolicy implements StealingPolicy {

	private int maximum=Integer.MAX_VALUE;
	private Map<Device, Integer> data=new HashMap<Device, Integer>();
	@Override
	public int jobsToSteal(Device stealer, Device victim) {
		int cant=1;
		if(this.data.containsKey(stealer))cant=this.data.get(stealer);
		if(cant>=victim.getWaitingJobs()) return victim.getWaitingJobs();
		if(cant>maximum) return maximum;
		this.data.put(stealer, cant*2);
		return cant;
	}
	
	public int getMaximum() {
		return this.maximum;
	}
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
	/**
	 * Version del seter para la conf
	 * @param maximum
	 */
	public void setMaximum(String maximum) {
		this.maximum = Integer.parseInt(maximum);
	}

}
