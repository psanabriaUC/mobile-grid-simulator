package edu.isistan.seas.proxy;


public class RemainingDataTransferingEvaluator implements
		DataAssignmentEvaluatorIF {

	@Override
	public double eval(DataAssignment da) {
		double remainingEnergy = da.getDevice().getJoulesBasedOnLastReportedSOC();
		for (int job_index = 0; job_index < da.getAssignedJobs().size(); job_index++) {
			remainingEnergy-=  da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getInputSize());
			remainingEnergy-= da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getOutputSize());
		}	
		
		return remainingEnergy;		
	}
	
}
