package edu.isistan.gridgain.information.comparator.tradeoff;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DeviceComparator;

public class StaticComparatorBase extends DeviceComparator {

	protected Map<Long, Double> properties;
	
	public StaticComparatorBase(String propFile){
		properties = new HashMap<Long, Double>();
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(propFile));
			for(String k:p.stringPropertyNames())
				properties.put(Long.parseLong(k), Double.parseDouble(p.getProperty(k)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	@Override
	public double getValue(Device arg0) {
		return ((double)arg0.getLastBatteryLevelUpdate())/properties.get(arg0.getMIPS())/((double)(arg0.getNumberOfJobs()+1));
	}
}
