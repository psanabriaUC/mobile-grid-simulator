package edu.isistan.gridgain.information.comparator.seas;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DefaultSEASComparator;

public class SEASPercentBased extends DefaultSEASComparator{

	public double getValue(Device arg0) {
		double mips=arg0.getMIPS();
		double uptime=arg0.getLastBatteryLevelUpdate();
		double nJobs= arg0.getIncommingJobs() + arg0.getNumberOfJobs()+1;
		return (mips*uptime)/nJobs;
	}

	
}
