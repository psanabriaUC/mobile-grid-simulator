package edu.isistan.seas.proxy.bufferedproxy;

import java.util.ArrayList;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.seas.proxy.DataIntensiveScheduler;


public abstract class BufferedSchedulerProxy extends DataIntensiveScheduler {

	protected ArrayList<Job> bufferedJobs = null;

	// when queue threshold equals one, the BufferedScheduler perform an
	// assignment every time a new job arrives,
	// otherwise, it waits until the queue has the number of jobs indicated by
	// this field to start job assignments.
	// setting a value greater than one, allows the scheduler to assign jobs in
	// an order different from the job arrival
	protected int bufferSize = 1;

	public BufferedSchedulerProxy(String name, String bufferValue) {
		super(name);
		if (bufferValue.compareTo("") != 0) {
			bufferSize = Integer.parseInt(bufferValue);
			System.out.println("BufferedScheduler was created with a buffer size value of: " + bufferSize);
		} else
			System.out.println(
					"[WARN] Since no buffer size was provided BufferedScheduler was created with a default buffer size of "
							+ bufferSize);
		bufferedJobs = new ArrayList<>();
	}

	@Override
	protected void assignJob(Job job) {
		queueJob(job);
		if (bufferedJobs.size() == bufferSize) {
			assignBufferedJobs();
			bufferedJobs.clear();
		}
	}

	protected abstract void queueJob(Job job);

	protected abstract void assignBufferedJobs();
	
}
