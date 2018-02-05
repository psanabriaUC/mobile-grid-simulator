package edu.isistan.seas.proxy.bufferedproxy;

import java.util.Comparator;

import edu.isistan.mobileGrid.jobs.Job;

public class AscendingAggregatedJobDataComparator implements Comparator<Job> {

	@Override
	public int compare(Job arg0, Job arg1) {
		long arg0value=arg0.getInputSize()+arg0.getOutputSize();
		long arg1value=arg1.getInputSize()+arg1.getOutputSize();
		
		if (arg0value < arg1value) return -1;
		if (arg0value > arg1value) return 1;
		
		return 0;
	}

}
