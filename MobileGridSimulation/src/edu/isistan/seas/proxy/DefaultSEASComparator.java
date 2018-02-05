package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.node.Device;

public class DefaultSEASComparator extends DeviceComparator {

	public double getValue(Device arg0) {
		double mips=arg0.getMIPS();
		double uptime=arg0.getEstimatedUptime();
		double nJobs=arg0.getNumberOfJobs()+1;
		return (mips*uptime)/nJobs;
	}

	
}
