package edu.isistan.seas.proxy;

import java.util.ArrayList;
import java.util.Collection;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;

public class RoundRobinProxy extends SchedulerProxy {

	private int next=0;
	protected int idSend=0;
	public RoundRobinProxy(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void processEvent(Event e) {
		if(EVENT_JOB_ARRIVE!=e.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job j=(Job)e.getData();
		JobStatsUtils.addJob(j, this);
		Logger.logEntity(this, "Job arrived ", j.getJobId());		
		if(next>=this.devices.size()) next=0;
		
		Device[] devs = new Device[this.devices.size()]; 
		this.devices.values().toArray(devs);
		Device current = devs[next];
		next++;
		
		queueJobTransferring(current,j);		
	}

	@Override
	public boolean runsOnBattery() {		
		return false;
	}

}
