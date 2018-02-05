package edu.isistan.seas.proxy;

import java.util.Comparator;

import edu.isistan.mobileGrid.node.Device;

public abstract class DeviceComparator implements Comparator<Device> {

	@Override
	public int compare(Device arg0, Device arg1) {
		double a0=this.getValue(arg0);
		double a1=this.getValue(arg1);
		if(a0>a1) return 1;
		if(a0<a1) return -1;
		return 0;
	}

	public abstract double getValue(Device arg0);

	
}
