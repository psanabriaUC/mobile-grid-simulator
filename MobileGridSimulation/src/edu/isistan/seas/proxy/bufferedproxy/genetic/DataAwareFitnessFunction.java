package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.proxy.DataAssignment;
import edu.isistan.seas.proxy.dataevaluator.OverflowPenaltyDataEvaluator;

public class DataAwareFitnessFunction extends FitnessFunction{

			
	
	public DataAwareFitnessFunction(int maxAvailable) {		
		this.setMaxEnergyAllowedForDataTransfer(maxAvailable);		
		DataAssignment.evaluator = new OverflowPenaltyDataEvaluator(getMaxEnergyAllowedForDataTransfer());
	}	
	
	public DataAwareFitnessFunction() {
		DataAssignment.evaluator = new OverflowPenaltyDataEvaluator(getMaxEnergyAllowedForDataTransfer());
	}	
	
	public FitnessValue getFitness(Short[] individual){
		HashMap<Integer,DataAssignment> deviceAssignments = convertIntoDeviceAssignments(individual);
		
		double gridDataTransfered = 0.0d;
		int gridJobsTransfered = 0;
		double gridEnergyUsed = 0.0d;
		double gridAvailableEnergy = SchedulerProxy.PROXY.getCurrentAggregatedNodesEnergy();		
		
		double[] nodesEnergySpent = new double[devicesId.size()];
		Arrays.fill(nodesEnergySpent, 0.0);		
		
		for (Iterator<DataAssignment> iterator = deviceAssignments.values().iterator(); iterator.hasNext();) {
			DataAssignment da = (DataAssignment) iterator.next();				
			DataAssignment.evaluator.eval(da);
			gridDataTransfered += da.getAffordableDataTranfered();
			gridJobsTransfered += da.getAffordableJobCompletelyTransfered();
			double devEnergy = da.getDeviceEnergyWasted();				
			gridEnergyUsed+=devEnergy;
			Device dev=da.getDevice();
			short devId=getDeviceId(dev);
			nodesEnergySpent[devId]+=devEnergy;
			
			//Logger.println("Expected node energy after job data transferrings:"+((Entity)dev).getName()+" "+(dev.getJoulesBasedOnLastReportedSOC()-nodesEnergySpent[devId]));
		}
		
		double gridEnergyRemainingPercentage = (gridAvailableEnergy - gridEnergyUsed)/ gridAvailableEnergy;
		
		//normalized data transfered			
		gridDataTransfered = gridDataTransfered / gar.getTotalDataToBeSchedule();
		//normalized job transfered
		double gridJobsTransferedPercentage = (gridJobsTransfered / gar.getGenesAmount());
		//normalized energy wasted
		gridEnergyUsed /= gridAvailableEnergy;
		//deviation nodes energy spent
		//double nodesEnergySpentDeviation = evaluateAssignmentsEnergyFairness(nodesEnergySpent);
		
		//+ gridDataTransfered + gridEnergyRemaining
		//(0.8*(gridJobsTransfered + gridDataTransfered)+ 0.2*gridEnergyRemaining);
		//return gridJobsTransfered + gridDataTransfered;// - nodesEnergySpentDeviation);
		//Logger.println("Fitness info: jobTransfered="+gridJobsTransfered+" gridRemainingEnergy="+gridEnergyRemaining);		
		FitnessValue fitness = new DataFitnessValue(gridJobsTransferedPercentage + gridEnergyRemainingPercentage,gridJobsTransfered,gridEnergyRemainingPercentage);
		
		return fitness;		
	}
	

	//assignment fairness is the standard deviation of energy spend by all nodes
	private double evaluateAssignmentsEnergyFairness(double[] nodesEnergySpend) {
		
		nodesEnergySpend = transformIntoPercentages(nodesEnergySpend);
		
		double sum = 0;
		for (int i = 0; i < nodesEnergySpend.length; i++)
			sum += nodesEnergySpend[i];		
		double mean = sum / nodesEnergySpend.length;
		
		double sumOfsqrtComponents = 0;		
		for (int i = 0; i < nodesEnergySpend.length; i++)
			sumOfsqrtComponents += Math.pow(nodesEnergySpend[i]-mean, 2);
		
		return Math.sqrt(sumOfsqrtComponents / nodesEnergySpend.length);
	}


	private double[] transformIntoPercentages(double[] nodesEnergySpend) {
		double[] nodesPerSpent = new double[devicesId.size()];
		Arrays.fill(nodesPerSpent, 0.0);
		
		for (int i=0; i < nodesEnergySpend.length; i++){
			Device dev = devicesId.get(i);
			double perValue = (double)(nodesEnergySpend[i] / dev.getInitialJoules());//(revisar, tal vez sea getBatteryCapacityInJoules el valor que tenga que utilizarse)
			nodesPerSpent[i]=perValue;
		}
		return nodesPerSpent;
	}
	
}
