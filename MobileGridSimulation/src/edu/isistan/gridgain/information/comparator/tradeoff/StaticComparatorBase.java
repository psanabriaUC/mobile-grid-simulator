package edu.isistan.gridgain.information.comparator.tradeoff;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DeviceComparator;

public class StaticComparatorBase extends DeviceComparator {

	protected Map<Long, Double> properties;
	
	public StaticComparatorBase(String propFile) {
		properties = new HashMap<>();
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(propFile));
			for(String property : properties.stringPropertyNames())
				this.properties.put(Long.parseLong(property), Double.parseDouble(properties.getProperty(property)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	@Override
	public double getValue(Device device) {
		double nJobs = SchedulerProxy.PROXY.getIncomingJobs(device) + device.getNumberOfJobs() + 1;
		return ((double)SchedulerProxy.PROXY.getLastReportedSOC(device)) / properties.get(device.getMIPS()) / nJobs;
	}
}
