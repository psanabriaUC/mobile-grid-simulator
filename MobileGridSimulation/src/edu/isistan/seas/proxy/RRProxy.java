package edu.isistan.seas.proxy;

import java.util.Collection;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class RRProxy extends SchedulerProxy {

	private int next = 0;

	public RRProxy(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void processEvent(Event event) {
		if(EVENT_JOB_ARRIVE!= event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job job = (Job) event.getData();
		JobStatsUtils.addJob(job, this);
		Logger.logEntity(this, "Job arrived ", job.getJobId());
		
		if(devices.size() == 0) {
			JobStatsUtils.rejectJob(job,Simulation.getTime());
			Logger.logEntity(this, "Job rejected = " + job.getJobId() + " at " + Simulation.getTime() +
                    " simulation time");
			return;
		}
		
		if (next >= devices.size()) next=0;
		
		Collection<Device> deviceList = this.devices.values();
		Device[] devArray = new Device[deviceList.size()];
		deviceList.toArray(devArray);
		Device current = devArray[next];
		next++;
		Logger.logEntity(this, "Job assigned to ", job.getJobId() ,current);

		queueJobTransferring(current, job);
		// NetworkModel.getModel().send(this, current, idSend++, job.getInputSize(), job);
	}


	@Override
	public boolean runsOnBattery() {		
		return false;
	}

}
