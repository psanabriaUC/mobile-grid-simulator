package edu.isistan.seas.proxy;

import java.util.Collection;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
//import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class RRProxy extends SchedulerProxy {

	private int next=0;
	protected int idSend=0;
	public RRProxy(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void processEvent(Event e) {
		if(EVENT_JOB_ARRIVE!=e.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job j=(Job)e.getData();
		JobStatsUtils.addJob(j, this);
		Logger.logEntity(this, "Job arrived ", j.getJobId());
		
		if(devices.size() == 0) {
			JobStatsUtils.rejectJob(j,Simulation.getTime());
			Logger.logEntity(this, "Job rejected = "+j.getJobId()+ " at "+Simulation.getTime()+ " simulation time");
			return;
		}
		
		if(next>=devices.size()) next=0;
		
		Collection<Device> deviceList = this.devices.values();
		Device[] devArray= new Device[deviceList.size()];
		deviceList.toArray(devArray);
		Device current=devArray[next];
		next++;
		Logger.logEntity(this, "Job assigned to ", j.getJobId() ,current);
		NetworkModel.getModel().send(this, current, idSend++, j.getInputSize(), j);
	}


	@Override
	public boolean runsOnBattery() {		
		return false;
	}

}
