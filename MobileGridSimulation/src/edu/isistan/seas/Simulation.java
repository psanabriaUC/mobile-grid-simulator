package edu.isistan.seas;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.SimpleNetworkModel;
import edu.isistan.mobileGrid.network.WifiLink;
import edu.isistan.mobileGrid.node.CloudNode;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.mobileGrid.node.TransferInfo;
import edu.isistan.mobileGrid.persistence.IPersisterFactory;
import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;
import edu.isistan.mobileGrid.persistence.DBEntity.JobStatsTuple;
import edu.isistan.persistence.mybatis.MybatisPersisterFactory;
import edu.isistan.seas.reader.DeviceReader;
import edu.isistan.seas.reader.SimReader;
import edu.isistan.simulator.Logger;


public class Simulation {
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if(args.length==3){
			Object o=new Object();
			System.err.println("Waiting for 10 sec");
			synchronized (o) {
				try {
					o.wait(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			o=null;
			System.err.println("Executing");
		}		
		setPersisters();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
				arg1.printStackTrace();
				System.exit(1);
			}
		});
		
		//uncomment for debugging
		/**OutputStream debugFile = null;
		try {
			debugFile = new FileOutputStream("DebugLog.log");
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}		
		Logger.setDebugOutputStream(debugFile);
		*/
		
		//((SimpleNetworkModel)NetworkModel.getModel()).setDefaultLink(new IdealBroadCastLink());
		
		
		boolean storeInDB=false;
		if (args.length == 2) storeInDB = Boolean.parseBoolean(args[1]);

		SimReader simReader = new SimReader();
		
		simReader.read(args[0], storeInDB);
		JobStatsUtils.setSim_id(SimReader.getSim_id());
		
		String cnfPath = args[0];
		String[] cnfPathArr = cnfPath.split("/");
		cnfPath = cnfPathArr[cnfPathArr.length-1];
		cnfPathArr = cnfPath.split("-");
		cnfPath = cnfPathArr[0];
		Logger.EXPERIMENT = cnfPath;

		CloudNode cloudNode = CloudNode.getInstance();
		Set<Node> cloudNodeSet = new HashSet<>();
		cloudNodeSet.add(cloudNode);
		
		for (Node node  : NetworkModel.getModel().getNodes()) {
			if(node instanceof Device) {
				short rssi = ((Device)node).getWifiRSSI();
				Set<Node> nodeSet = new HashSet<Node>();
				nodeSet.add(node);
				
				Set<Node> proxySet = new HashSet<Node>();
				proxySet.add(SchedulerProxy.PROXY);
				
				WifiLink wl1 = new WifiLink(rssi, nodeSet, proxySet);
				WifiLink wl2 = new WifiLink(rssi, proxySet, nodeSet);
				WifiLink wl3 = new WifiLink(rssi, nodeSet, cloudNodeSet);

				
				NetworkModel.getModel().addNewLink(wl1);
				NetworkModel.getModel().addNewLink(wl2);
                NetworkModel.getModel().addNewLink(wl3);
			}
		}

		edu.isistan.simulator.Simulation.runSimulation();
		
		
		if (storeInDB) {
			JobStatsUtils.storeInDB();
		}
		
		//Logger.flushDebugInfo();
		
		JobStatsUtils.printNodeInformationSummaryByNodeMips();
		System.out.print("Total simulated time: ");
		System.out.println(edu.isistan.simulator.Simulation.getTime());
		System.out.println(JobStatsUtils.timeToHours(edu.isistan.simulator.Simulation.getTime()));
		System.out.print("Jobs simulated: ");
		System.out.println(JobStatsUtils.getSize());
		System.out.print("Jobs successfully executed: ");
		System.out.println(JobStatsUtils.getSuccessfullyExecutedJobs());		
		System.out.print("Successfully execution time: ");
		System.out.println(JobStatsUtils.getEffectiveExecutionTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getEffectiveExecutionTime()));
		System.out.print("Successfully execution time per effective job: ");
		int execJobs = JobStatsUtils.getSuccessfullyExecutedJobs();
		if (execJobs > 0) {
			System.out.println(JobStatsUtils.getEffectiveExecutionTime()/execJobs);
			System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getEffectiveExecutionTime()/execJobs));
		} else {
			System.out.println(JobStatsUtils.timeToHours(execJobs));
			System.out.println(JobStatsUtils.timeToHours(execJobs));
		}
		
		
		System.out.print("Executed job waiting time: ");
		System.out.println(JobStatsUtils.getEffectiveQueueTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getEffectiveQueueTime()));
		System.out.print("Executed job waiting time per effective: ");
		if (execJobs > 0){
			System.out.println(JobStatsUtils.getEffectiveQueueTime()/JobStatsUtils.getSuccessfullyExecutedJobs());
			System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getEffectiveQueueTime()/JobStatsUtils.getSuccessfullyExecutedJobs()));
		} else {
			System.out.println(execJobs);
			System.out.println(JobStatsUtils.timeToHours(execJobs));
		}

		System.out.println("*****************************");
		System.out.print("Total queue time: ");
		System.out.println(JobStatsUtils.getTotalQueueTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getTotalQueueTime()));
		long avgQueueTimePerJob = JobStatsUtils.getSize() == 0 ? 0 : JobStatsUtils.getTotalQueueTime()/JobStatsUtils.getSize();
		System.out.println("Average queue time per job: "+avgQueueTimePerJob);
		
		System.out.println(JobStatsUtils.timeToHours(avgQueueTimePerJob));
		System.out.print("Total execution time: ");
		System.out.println(JobStatsUtils.getTotalExecutionTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getTotalExecutionTime()));
		
		long avgExecTimePerJob = JobStatsUtils.getSize() == 0 ? 0 : JobStatsUtils.getTotalExecutionTime()/JobStatsUtils.getSize();
		System.out.println("Average execution time per job: "+avgExecTimePerJob);		
		System.out.println(JobStatsUtils.timeToHours(avgExecTimePerJob));
		
		System.out.println("*****************************");
		int failed = JobStatsUtils.getSize() - JobStatsUtils.getSuccessfullyExecutedJobs();
		long wastedWaited = JobStatsUtils.getTotalQueueTime() - JobStatsUtils.getEffectiveQueueTime();
		long wastedExecution = JobStatsUtils.getTotalExecutionTime() - JobStatsUtils.getEffectiveExecutionTime();
		System.out.print("Failed jobs: ");
		System.out.println(failed);
		System.out.print("Wasted queue time: ");
		System.out.println(wastedWaited);
		System.out.println(JobStatsUtils.timeToHours(wastedWaited));
		System.out.print("Average wasted queue time per failed job: ");		 
		long wastedDivFailed = failed != 0 ? wastedWaited/failed : 0; 
		System.out.println(wastedDivFailed);
		String wasteDivFailedInHours = failed != 0 ? JobStatsUtils.timeToHours(wastedWaited/failed): "0";
		System.out.println(wasteDivFailedInHours);
		System.out.print("Wasted execution time: ");
		System.out.println(wastedExecution);
		System.out.println(JobStatsUtils.timeToHours(wastedExecution));
		System.out.print("Average wasted execution time per failed job: ");
		wastedDivFailed = failed != 0 ? wastedExecution/failed : 0;
		System.out.println(wastedDivFailed);
		wasteDivFailedInHours = failed != 0 ? JobStatsUtils.timeToHours(wastedExecution/failed): "0"; 
		System.out.println(wasteDivFailedInHours);		
		
		System.out.println("*****************************");
		System.out.print("Total transfers: ");
		System.out.println(JobStatsUtils.cantJobTrasnfers());
		System.out.print("Total stealings: ");		
		System.out.println(JobStatsUtils.getTotalStealings());
		System.out.print("Total transfer time: ");
		System.out.println(JobStatsUtils.getTotalTransferTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getTotalTransferTime()));
		long jobsAverageTransferTime = JobStatsUtils.getSize() == 0 ? 0 : JobStatsUtils.getTotalTransferTime()/JobStatsUtils.getSize();
		System.out.println("Total transfer time per job: "+jobsAverageTransferTime);		
		System.out.println(JobStatsUtils.timeToHours(jobsAverageTransferTime));
		System.out.println("*****************************");
		System.out.print("Total results transfers: ");
		System.out.println(JobStatsUtils.cantJobResultTransfers());
		System.out.print("Total result transfer time: ");
		System.out.println(JobStatsUtils.getTotalResultsTransferTime());
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getTotalResultsTransferTime()));
		System.out.print("Total result transfer time per job: ");
		if (execJobs > 0){
			System.out.println(JobStatsUtils.getTotalResultsTransferTime()/JobStatsUtils.getSuccessfullyExecutedJobs());
			System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.getTotalResultsTransferTime()/JobStatsUtils.getSuccessfullyExecutedJobs()));
		}
		else{
			System.out.println(execJobs);
			System.out.println(JobStatsUtils.timeToHours(execJobs));
		}
			
		System.out.println("*****************************");
		System.out.println("Net stats summary");
		System.out.println("-------------------");
		System.out.println("Total Percentage of energy consumed in data transmisions: "+JobStatsUtils.getPercentageOfEnergyInDataTransmision());
		System.out.println("Percentage sending:"+JobStatsUtils.getPercentageOfEnergyInSendingData());
		System.out.println("Percentage receiving:"+JobStatsUtils.getPercentageOfEnergyInReceivingData());		
		System.out.print("Total update messages received by the proxy:");		
		System.out.println(JobStatsUtils.getTotalUpdateMsgReceivedByProxy());
		System.out.print("Total update messages sent by nodes:");
		System.out.println(JobStatsUtils.getTotalUpdateMsgSentByNodes());
		System.out.print("Amount of sent data (in Gb):");
		System.out.println(JobStatsUtils.getTotalTransferedData(true)/1024);
		System.out.print("Amount of received data (in Gb):");
		System.out.println(JobStatsUtils.getTotalTransferedData(false)/1024);
		System.out.print("Total job data input (in Gb):");
		System.out.println(JobStatsUtils.getAggregatedJobsData(true));
		System.out.print("Total job data output (in Gb):");
		System.out.println(JobStatsUtils.getAggregatedJobsData(false));
		System.out.print("Percent of transfered data:");
		System.out.println(JobStatsUtils.getPercentOfTransferedData());		
		System.out.println("*****************************");		
		System.out.println(JobStatsUtils.printNodesPercentageOfEnergyWasteInNetworkActivity());
		System.out.println("Jobs states summary");
		System.out.println("-------------------");
		JobStatsUtils.printJobStatesSummary();
		System.out.print("Percentage of completed jobs:");
		System.out.println(((((Integer)(JobStatsUtils.getCompletedJobs()*100)).floatValue()))/((Integer)JobStatsUtils.getSize()).floatValue());
		System.out.print("Nodes iddle Time:");		
		System.out.println(JobStatsUtils.timeToHours(JobStatsUtils.devicesIddleTime));
		System.out.print("Total executed ops (in GIPs):");		
		System.out.println(JobStatsUtils.getTotalExecutedGIP());
		//System.out.println("*****************************");
		//System.out.println(((SimpleGASchedulerProxy)SchedulerProxy.PROXY).printGeneticRoundsInfo());		
		
		//ValidateExperiment();
	}

	private static void setPersisters() {
		IPersisterFactory persistenceFactory = new MybatisPersisterFactory();
		DeviceReader.setPersisterFactory(persistenceFactory);
		JobStatsTuple.setIPersisterFactory(persistenceFactory);
		JobStatsUtils.persisterFactory = persistenceFactory;
		DeviceTuple.setIPersisterFactory(persistenceFactory);
		SimReader.setPersisterFactory(persistenceFactory);
		
	}
	
	private static void validateExperiment()
	{
		//Validate: Devices Initial energy is enough for Jobs Assignments
		java.util.HashMap<Device, Double> device_assignEnergy = new java.util.HashMap<Device, Double>();
		java.util.HashMap<Device, Long> device_assignTime = new java.util.HashMap<Device, Long>();
		Collection<TransferInfo> transfers = SchedulerProxy.PROXY.getTransfersCompleted();
		transfers.addAll(SchedulerProxy.PROXY.getTransfersPending());
		
		boolean valid_assigment = true;
		for (TransferInfo tInfo : transfers) {
			// FIXME: unsafe
			Device device = (Device) tInfo.getDestination();
			Job job = (Job) tInfo.getData();

			double energy  = device_assignEnergy.containsKey(device)?device_assignEnergy.get(device):0;
			energy += device.getEnergyWasteInTransferringData(job.getInputSize());
			energy += device.getEnergyWasteInTransferringData(job.getOutputSize());
			device_assignEnergy.put(device, energy);
			
			long time = device_assignTime.containsKey(device) ? device_assignTime.get(device) : 0;
			
			int data = job.getInputSize();
			long subMessagesCount = (long) Math.ceil(data/(double)Device.MESSAGES_BUFFER_SIZE);
			long lastMessageSize = (long)data - (subMessagesCount-1)*Device.MESSAGES_BUFFER_SIZE;
			time += (subMessagesCount-1)*NetworkModel.getModel().getTransmissionTime(SchedulerProxy.PROXY, device, (int)Device.MESSAGES_BUFFER_SIZE);
			time += NetworkModel.getModel().getTransmissionTime(SchedulerProxy.PROXY, device, (int)lastMessageSize);

			data = job.getOutputSize();
		    subMessagesCount = (long) Math.ceil(data/(double)Device.MESSAGES_BUFFER_SIZE);
			lastMessageSize = (long)data - (subMessagesCount-1)*Device.MESSAGES_BUFFER_SIZE;
			time += (subMessagesCount-1)*NetworkModel.getModel().getTransmissionTime(device, SchedulerProxy.PROXY, (int)Device.MESSAGES_BUFFER_SIZE);
			time += NetworkModel.getModel().getTransmissionTime(device, SchedulerProxy.PROXY, (int)lastMessageSize);
			
			device_assignTime.put(device, time);
			
			if(device.getInitialJoules() < energy) {
				valid_assigment = false;
			}
		}
		
		//Validate: Devices AccEnergyInTransfering corresponds to assignments expected energy waste
		boolean valid_energy_simulation = true;
		double [] energyDiffs = new double[100];
		
		//Validate: Devices, time between first input and last output correspond with sum of expected times of all msg  
		boolean valid_time_simulation = true;
		double [] timeDiffs = new double[100];
		
		int  i = 0;
		for (Device device : device_assignEnergy.keySet()) {
			double assign_energy = device_assignEnergy.get(device);
			double waste_energy = device.getAccEnergyInTransferring();
			double ack_energy =  device.getFinishedJobTransfersCompleted().size() * device.getEnergyWasteInTransferringData(NetworkModel.getModel().getAckMessageSizeInBytes());
			if(Math.abs(assign_energy - waste_energy) > 0.01){	//+ ack_energy
				valid_energy_simulation = false;
			}
			
			energyDiffs[i] = assign_energy - waste_energy;
	
			long expectedTransferingTime = device_assignTime.get(device);
			
			long lastTime = ((SimpleNetworkModel)NetworkModel.getModel()).lastTransferringTimes.get(device);
			long firstTime = ((SimpleNetworkModel)NetworkModel.getModel()).firstTransferringTimes.get(device);
			long transferingTime = lastTime - firstTime;
			
			if(Math.abs(expectedTransferingTime - transferingTime) > 0.01){
				valid_time_simulation = false;
			}
			
			timeDiffs[i] = (expectedTransferingTime - transferingTime)/1000.0;
			i++;
		}		
		
		System.out.println("---------------------------Validations-------------------------");
		System.out.println((valid_assigment?"Valid":"Invalid")+" jobs assigments based on initial devices energy");
		System.out.println((valid_energy_simulation?"Valid":"Invalid")+" simulation energy discount based on assigments");
		System.out.println((valid_time_simulation?"Valid":"Invalid")+" simulation time transfers based on assigments");
		
	}
}
