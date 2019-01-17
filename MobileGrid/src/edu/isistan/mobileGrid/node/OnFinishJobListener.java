package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.jobs.Job;

public interface OnFinishJobListener {
    void finished(Device device, Job job);
}
