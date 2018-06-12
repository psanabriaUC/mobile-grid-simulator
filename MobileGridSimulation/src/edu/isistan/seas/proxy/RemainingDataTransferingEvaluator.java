package edu.isistan.seas.proxy;


import edu.isistan.mobileGrid.node.SchedulerProxy;

public class RemainingDataTransferingEvaluator implements
		DataAssignmentEvaluatorIF {

	@Override
	public double eval(DataAssignment da) {
		double remainingEnergy = SchedulerProxy.PROXY.getJoulesBasedOnLastReportedSOC(da.getDevice());
		for (int job_index = 0; job_index < da.getAssignedJobs().size(); job_index++) {
			remainingEnergy -=  da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getInputSize());
			remainingEnergy -= da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getOutputSize());
		}	
		
		return remainingEnergy;		
	}
	
}
