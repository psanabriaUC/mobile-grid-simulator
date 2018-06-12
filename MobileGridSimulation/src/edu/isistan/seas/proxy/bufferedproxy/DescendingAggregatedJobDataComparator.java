package edu.isistan.seas.proxy.bufferedproxy;

import java.util.Comparator;

import edu.isistan.mobileGrid.jobs.Job;

public class DescendingAggregatedJobDataComparator implements Comparator<Job> {

	@Override
	public int compare(Job job1, Job job2) {
		long job1value = job1.getInputSize() + job1.getOutputSize();
		long job2value = job2.getInputSize() + job2.getOutputSize();

		return Long.compare(job2value, job1value);

	}

}
