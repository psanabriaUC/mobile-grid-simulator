package edu.isistan.edge.proxy.jobstealing;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DefaultSEASComparator;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

import java.util.HashMap;

public class EdgeStealerProxy extends StealerProxy {
    private HashMap<String, Long> assignedJobs;

    public EdgeStealerProxy(String name) {
        super(name);
        assignedJobs = new HashMap<>();
        devComp = new DefaultSEASComparator();
    }

    @Override
    protected void assignJob(Job job) {
        if (this.devices.isEmpty()) {
            JobStatsUtils.rejectJob(job, Simulation.getTime());
            Logger.logEntity(this, "Job rejected = " + job.getJobId() + " at " + Simulation.getTime() +
                    " simulation time");
        } else {
            Device selectedDevice = null;
            for (Device device : this.devices.values()) {
                if (!device.runsOnBattery() && (selectedDevice == null || devComp.compare(device, selectedDevice) <= 0)) {
                    selectedDevice = device;
                }
            }

            if (selectedDevice != null) {
                queueJobTransferring(selectedDevice, job);
                assignedJobs.put(selectedDevice.getName(), assignedJobs.get(selectedDevice.getName()) + job.getOps());
                if (!selectedDevice.runsOnBattery())
                    job.setFromEdge(true);
            }
        }
    }

    @Override
    public boolean runsOnBattery() {
        return false;
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
    public void onMessageReceived(Message message) {
        super.onMessageReceived(message);
        if (message.getData() instanceof UpdateMsg) {
            UpdateMsg msg = (UpdateMsg)message.getData();
            Device device = devices.get(msg.getNodeId());

            if (device.getWaitingJobs() == 0) {
                ((StealerProxy) SchedulerProxy.PROXY).steal(device);
            }
        }
    }
}
