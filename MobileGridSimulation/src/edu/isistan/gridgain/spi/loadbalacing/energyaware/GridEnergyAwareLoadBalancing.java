package edu.isistan.gridgain.spi.loadbalacing.energyaware;

import java.util.Collection;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DefaultSEASComparator;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class GridEnergyAwareLoadBalancing extends SchedulerProxy {

	protected DeviceComparator devComp = new DefaultSEASComparator();
	
	public GridEnergyAwareLoadBalancing(String name) {
		super(name);
	}
	
	@Override
	public void processEvent(Event event) {
		if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job job = (Job) event.getData();
		JobStatsUtils.addJob(job, this);
		Logger.logEntity(this, "Job arrived ", job.getJobId());
		assignJob(job);
	}

    /**
     * Assigns a job to the device in the grid with the highest node rank according to the SEAS algorithm.
     *
     * @param job The job to assign.
     */
	protected void assignJob(Job job) {
		if (this.devices.values().isEmpty()) {
			JobStatsUtils.rejectJob(job,Simulation.getTime());
			Logger.logEntity(this, "Job rejected = " + job.getJobId() + " at " + Simulation.getTime() +
					" simulation time");
		} else {
			Device selectedDevice = null;
			for (Device device : this.devices.values()) {
				if (selectedDevice == null || this.devComp.compare(device, selectedDevice) > 0) {
					selectedDevice = device;
				}
			}
			queueJobTransferring(selectedDevice, job);
		}
	}

	public DeviceComparator getDevComp() {
		return this.devComp;
	}

	public void setDevComp(DeviceComparator devComp) {
		this.devComp = devComp;
		Logger.logEntity(this, "Using Comparator", devComp.getClass().getName());
	}

	@Override
	public boolean runsOnBattery() {		
		return false;
	}
}
