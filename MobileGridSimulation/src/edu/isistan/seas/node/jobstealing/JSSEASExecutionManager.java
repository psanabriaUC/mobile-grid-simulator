package edu.isistan.seas.node.jobstealing;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.node.DefaultExecutionManager;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;

public class JSSEASExecutionManager extends DefaultExecutionManager {

	@Override
	public void onFinishJob(Job job) {
		super.onFinishJob(job);
		if(this.getJobQueueSize() == 0 && !isExecuting()) {
			((StealerProxy) SchedulerProxy.PROXY).steal(this.getDevice());
		}
	}

}
