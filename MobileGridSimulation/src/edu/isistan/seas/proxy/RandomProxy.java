package edu.isistan.seas.proxy;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;

import java.util.Random;

public class RandomProxy extends SchedulerProxy {
    private Random random = new Random();
	protected int idSend = 0;
	
	public RandomProxy(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void processEvent(Event event) {
		if(EVENT_JOB_ARRIVE!= event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job job = (Job) event.getData();
		JobStatsUtils.addJob(job, this);
		Logger.logEntity(this, "Job arrived ", job.getJobId());

        String[] keys = this.devices.keySet().toArray(new String[this.devices.size()]);
		Device current = this.devices.get(keys[random.nextInt(keys.length)]);

		Logger.logEntity(this, "Job assigned to ", job.getJobId() ,current);
		queueJobTransferring(current, job);

	}

	@Override
	public boolean runsOnBattery() {
		//TODO: revisar esta respuesta
		return false;
	}

}
