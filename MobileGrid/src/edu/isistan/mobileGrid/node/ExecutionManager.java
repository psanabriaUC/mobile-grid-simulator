package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.jobs.Job;

public interface ExecutionManager {
	/**
	 * Call when a new jobs arrives
	 * @param job
	 */
	public void addJob(Job job);
	/**
	 * get the number of jobs enqueue
	 * @return
	 */
	public int getJobQueueSize();

	/**
	 * removes a job
	 * @param index
	 */
	public Job removeJob(int index);

	/**
	 * Call when a job is finished
	 * @param job
	 */
	public void onFinishJob(Job job);

	/**
	 * Call when a CPU event arrives
	 * @param cpuUsage
	 */
	public void onCPUEvent(double cpuUsage);

	/**
	 * Get number of jobs
	 * @return
	 */
	public int getNumberOfJobs();
	/**
	 * Get cpu mips
	 * @return
	 */
	public long getMIPS();
	/**
	 * Get current cpu usage
	 * @return
	 */
	public double getCPUUsage();
	/**
	 * Call when the device shutdown
	 */
	public void shutdown();
}
