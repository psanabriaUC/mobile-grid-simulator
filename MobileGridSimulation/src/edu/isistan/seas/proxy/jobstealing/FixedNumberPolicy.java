package edu.isistan.seas.proxy.jobstealing;

import edu.isistan.mobileGrid.node.Device;

public class FixedNumberPolicy implements StealingPolicy {
	private int fixedNumer;

	@Override
	public int jobsToSteal(Device stealer, Device victim) {
		int cant=this.fixedNumer;
		if(cant>=victim.getWaitingJobs())cant=victim.getWaitingJobs();
		return cant;
	}

	public int getFixedNumer() {
		return this.fixedNumer;
	}

	public void setFixedNumer(int fixedNumer) {
		this.fixedNumer = fixedNumer;
	}
	
	/**
	 * Version del setter para la conf
	 * @param fixedNumer
	 */
	public void setFixedNum(String fixedNumer) {
		this.fixedNumer = Integer.parseInt(fixedNumer);
	}
	
	public void setFixedNumer(String fixedNumer) {
		this.fixedNumer = Integer.parseInt(fixedNumer.trim());
	}

}
