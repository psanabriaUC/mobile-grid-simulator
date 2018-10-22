package edu.isistan.edge.proxy;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DeviceComparator;

import java.util.HashMap;

public class BatchProcessingComparator extends DeviceComparator {
    private HashMap<String, Long> jobsLoad;

    public BatchProcessingComparator(HashMap<String, Long> jobsLoad) {
        this.jobsLoad = jobsLoad;
    }

    @Override
    public double getValue(Device device) {
        long ops = jobsLoad.get(device.getName());
        long flops = device.getMIPS();

        return (double)ops / flops;
    }
}
