package edu.isistan.edge.proxy.jobstealing;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;
import edu.isistan.seas.proxy.jobstealing.StealingStrategy;

import java.util.Collection;

public class EdgeWRAStrategy implements StealingStrategy {
    @Override
    public Device getVictim(StealerProxy sp, Device stealer) {
        Collection<Device> devices = sp.getDevices();

        if (devices.size() == 0)
            return null;

        DeviceComparator comparator = sp.getDevComp();
        Device current = null;

        for (Device next : devices) {
            if (!next.runsOnBattery()) {
                if (current == null) {
                    current = next;
                } else {
                    if ((comparator.compare(current, next) > 0) && (next.getWaitingJobs() > 0) && (next != stealer))
                        current = next;
                }
            }
        }

        if (current == null) return null;
        if (current.getWaitingJobs() == 0) return null;
        if (current == stealer) return null;

        return current;
    }
}
