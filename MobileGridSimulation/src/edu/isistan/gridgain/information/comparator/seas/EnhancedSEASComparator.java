package edu.isistan.gridgain.information.comparator.seas;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DeviceComparator;

/**
 * E-SEAS node rank implementation. Devices with a higher rank are given priority when assigning jobs.
 * For more information, see: <a href="https://link.springer.com/article/10.1007/s10723-016-9387-6">
 *     A Two-Phase Energy-Aware Scheduling Approach for CPU-Intensive Jobs in Mobile Grids</a>.
 */
public class EnhancedSEASComparator extends DeviceComparator {

	@Override
	public double getValue(Device device) {
		double mips = device.getMIPS();
		double uptime = SchedulerProxy.PROXY.getLastReportedSOC(device);
		double nJobs = SchedulerProxy.PROXY.getIncomingJobs(device) + device.getNumberOfJobs() + 1;
		return (mips * uptime) / nJobs;
	}

	
}
