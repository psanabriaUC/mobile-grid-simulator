package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.jobs.Job;

public interface ExecutionManager {
    /**
     * Call when a new jobs arrives
     *
     * @param job Job to add
     */
    void addJob(Job job);

    /**
     * get the number of jobs enqueue
     *
     * @return nothing
     */
    int getJobQueueSize();

    /**
     * removes a job
     *
     * @param index index of job to remove
     */
    Job removeJob(int index);

    /**
     * Call when a job is finished
     *
     * @param job
     */
    void onFinishJob(Job job);

    /**
     * Call when a CPU event arrives
     *
     * @param cpuUsage
     */
    void onCPUEvent(double cpuUsage);

    /**
     * Get number of jobs
     *
     * @return
     */
    int getNumberOfJobs();

    /**
     * Get cpu mips
     *
     * @return
     */
    long getMIPS();

    /**
     * Get current cpu usage
     *
     * @return
     */
    double getCPUUsage();

    /**
     * Call when the device shutdown
     */
    void shutdown();
}
