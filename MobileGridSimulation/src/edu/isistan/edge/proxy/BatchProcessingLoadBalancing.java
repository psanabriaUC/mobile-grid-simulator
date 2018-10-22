package edu.isistan.edge.proxy;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

import java.util.HashMap;

public class BatchProcessingLoadBalancing extends SchedulerProxy {
    private DeviceComparator comparator;
    private HashMap<String, Long> assignedJobs;

    public BatchProcessingLoadBalancing(String name) {
        super(name);
        assignedJobs = new HashMap<>();
        comparator = new BatchProcessingComparator(assignedJobs);
    }

    @Override
    public void processEvent(Event event) {
        if (event.getEventType() != EVENT_JOB_ARRIVE)
            throw new IllegalArgumentException("Unexpected event");
        Job job = (Job) event.getData();
        JobStatsUtils.addJob(job, this);
        Logger.logEntity(this, "Job arrived to edge proxy", job.getJobId());
        assignJob(job);
    }

    private void assignJob(Job job) {
        if (this.devices.isEmpty()) {
            JobStatsUtils.rejectJob(job, Simulation.getTime());
            Logger.logEntity(this, "Job rejected = " + job.getJobId() + " at " + Simulation.getTime() +
                    " simulation time");
        } else {
            Device selectedDevice = null;
            for (Device device : this.devices.values()) {
                if (selectedDevice == null || comparator.compare(device, selectedDevice) <= 0) {
                    selectedDevice = device;
                }
            }
            if (selectedDevice != null) {
                queueJobTransferring(selectedDevice, job);
                assignedJobs.put(selectedDevice.getName(), assignedJobs.get(selectedDevice.getName()) + job.getOps());

                if (!selectedDevice.runsOnBattery()) {
                    job.setFromEdge(true);
                }
            }
        }
    }

    @Override
    public void remove(Device device) {
        super.remove(device);
        assignedJobs.remove(device.getName());
    }

    @Override
    public void addDevice(Device device) {
        super.addDevice(device);
        assignedJobs.put(device.getName(), 0L);
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }
}
