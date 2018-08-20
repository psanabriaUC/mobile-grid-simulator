package edu.isistan.seas.node;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;

public class InfiniteExecutionManager extends DefaultExecutionManager {
    private long idleTime;

    @Override
    public void addJob(Job job) {
        super.addJob(job);
    }

    @Override
    public void onFinishJob(Job job) {
        super.onFinishJob(job);
        if (getJobQueueSize() == 0) {
            getBatteryManager().onBatteryEvent(0);
        }
        if (JobStatsUtils.getNumberOfJobsStarted() <= JobStatsUtils.getCompletedJobs()) {
            getBatteryManager().onBatteryEvent(0);
        }
    }
}
