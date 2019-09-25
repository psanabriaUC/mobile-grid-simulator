package edu.isistan.edge.proxy.jobstealing;

import edu.isistan.edge.proxy.BatchProcessingComparator;
import edu.isistan.edge.proxy.LoadComparator;
import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.OnFinishJobListener;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.node.jobstealing.JSMessage;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

import java.util.HashMap;

public class MyStealerProxy extends StealerProxy {
    private HashMap<String, Long> assignedJobs;
    private DeviceComparator initialComparator;

    public MyStealerProxy(String name) {
        super(name);
        assignedJobs = new HashMap<>();
        devComp = new LoadComparator(assignedJobs);
        initialComparator = new BatchProcessingComparator(assignedJobs);
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
                if (selectedDevice == null || initialComparator.compare(device, selectedDevice) <= 0) {
                    selectedDevice = device;
                }
            }

            if (selectedDevice != null) {
                queueJobTransferring(selectedDevice, job);
                long previousOps = assignedJobs.get(selectedDevice.getName());
                assignedJobs.put(selectedDevice.getName(), assignedJobs.get(selectedDevice.getName()) + job.getOps());
                Logger.logEntity(this, "JOBASSIGNED Assigned job of " + job.getOps() + " ops to "+ selectedDevice +" now assigned " + assignedJobs.get(selectedDevice.getName()) + " ops, previous was " + previousOps, job);
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
        device.setOnFinishJobListener(new OnFinishJobListener() {
            @Override
            public void finished(Device device, Job job) {
                long ops = assignedJobs.get(device.getName()) - job.getOps();
                Logger.logEntity(device, "JOBFINISHED Finished job of " + job.getOps() + " ops, now assigned "+ ops +" ops, previous was " + assignedJobs.get(device.getName()), job);
                if (ops < 0)
                    throw new IllegalStateException("Negative remaining OPS");
                assignedJobs.put(device.getName(), ops);
                Logger.logEntity(device, "" + assignedJobs);

            }
        });
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

    @Override
    protected void onJobStolen(Device stealer, Device victim, Job job) {
        super.onJobStolen(stealer, victim, job);

        long victimOps = assignedJobs.get(victim.getName()) - job.getOps();
        long stealerOps = assignedJobs.get(stealer.getName()) + job.getOps();

        if (victimOps < 0) {
            throw new IllegalStateException("Negative remaining OPS for Victim");
        }

        Logger.logEntity(this, "JOBSTEAL/STEALER Assigned job of " + job.getOps() + " ops to "+ stealer +" now assigned " + stealerOps + " ops, previous was " + assignedJobs.get(stealer.getName()), job);
        Logger.logEntity(this, "JOBSTEAL/VICTIM Stolen job of " + job.getOps() + " ops to "+ victim +" now assigned " + victimOps + " ops, previous was " + assignedJobs.get(victim.getName()), job);

        assignedJobs.put(stealer.getName(), stealerOps);
        assignedJobs.put(victim.getName(), victimOps);
    }
}
