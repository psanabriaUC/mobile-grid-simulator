package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomProxy extends SchedulerProxy {
    protected int idSend = 0;
    private long totalWeight = 0;
    private LinkedHashMap<Long, Device> weightMap = new LinkedHashMap<>();


    public WeightedRandomProxy(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = (Job) event.getData();
        JobStatsUtils.addJob(job, this);
        Logger.logEntity(this, "Job arrived ", job.getJobId());
        Device current = selectDevice();

        Logger.logEntity(this, "Job assigned to ", job.getJobId(), current);
        queueJobTransferring(current, job);

    }

    @Override
    public void addDevice(Device device) {
        super.addDevice(device);
        totalWeight += device.getMIPS();
        Logger.logEntity(this, "totalWeight ", totalWeight);
        weightMap.put(totalWeight, device);
    }

    private Device selectDevice() {
        long targetNumber = ThreadLocalRandom.current().nextLong(totalWeight);
        ArrayList<Long> weights = new ArrayList<>(weightMap.keySet());
        ArrayList<Device> devices = new ArrayList<>(weightMap.values());
        int begin = 0;
        int mid = 0;
        int end =  weights.size() - 1;

        while (begin <= end) {
            mid = (begin + end) / 2;

            if ( targetNumber <= weights.get(mid) && weights.get(mid) - devices.get(mid).getMIPS() <= targetNumber)
                break;
            else if ( targetNumber <= weights.get(mid) ) {
                end = mid - 1;
            } else {
                begin = mid + 1;
            }
        }
        return devices.get(mid);
    }

    @Override
    public boolean runsOnBattery() {
        //TODO: revisar esta respuesta
        return false;
    }

}
