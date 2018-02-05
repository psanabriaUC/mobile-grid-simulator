package edu.isistan.seas.proxy.jobstealing;

import java.util.Collection;

import edu.isistan.mobileGrid.node.Device;

public class RandomStrategy implements StealingStrategy {

	@Override
	public Device getVictim(StealerProxy sp, Device stealer) {
		Collection<Device> devs=sp.getDevices();
		Device[] devicesArray = new Device[devs.size()];
		devs.toArray(devicesArray);
		if(devs.size()==1) return null;
		Device d = stealer;
		int tries = 0;
		while (tries>1000){
			//TODO revisar
			d = devicesArray[((int) (devs.size()*Math.random()))];
			tries++;
			if (d==stealer) return null;
		}
		return d;
	}

}
