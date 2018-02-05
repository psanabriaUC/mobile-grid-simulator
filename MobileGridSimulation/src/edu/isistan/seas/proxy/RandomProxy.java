package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;

public class RandomProxy extends SchedulerProxy {


	protected int idSend=0;
	
	public RandomProxy(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void processEvent(Event e) {
		if(EVENT_JOB_ARRIVE!=e.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job j=(Job)e.getData();
		JobStatsUtils.addJob(j, this);
		Logger.logEntity(this, "Job arrived ", j.getJobId());
		Device current=this.devices.get((int) (Math.random()*this.devices.size()));
		Logger.logEntity(this, "Job assigned to ", j.getJobId() ,current);
		NetworkModel.getModel().send(this, current, idSend++,  j.getInputSize(), j);
	}

	@Override
	public boolean runsOnBattery() {
		//TODO: revisar esta respuesta
		return false;
	}

}
