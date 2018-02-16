package edu.isistan.seas.proxy.bufferedproxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.node.DefaultBatteryManager;
import edu.isistan.seas.proxy.RSSIDataJoulesEvaluator;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class BacktrackingBasedProxy extends BufferedSchedulerProxy {
	
	private double bestFitness = 0;
	private double currentFitnessValue = 0;
	private short deviceQuantity = 0;
	private HashMap<Short,ArrayList<Short>> currentAssignment;
	private HashMap<Short,Double>  accDataPerDevice;
	private HashMap<Short,Short[]> bestAssignment;
	private HashMap<Short,Device>  deviceIds;
	private RSSIDataJoulesEvaluator energyEvaluator = new RSSIDataJoulesEvaluator();
	
	public BacktrackingBasedProxy(String name, String bufferValue) {
		super(name, bufferValue);
		currentAssignment = new HashMap<Short,ArrayList<Short>>();
		bestAssignment = new HashMap<Short,Short[]>();
		accDataPerDevice = new HashMap<Short,Double>();
		deviceIds = new HashMap<Short,Device>();
	}

	@Override
	protected void queueJob(Job job) {
		bufferedJobs.add(job);
	}

	@Override
	protected void assignBufferedJobs() {
		mapDeviceswithIDs();
		initializeStructures();
		Long init = System.currentTimeMillis();		
		generateBestAssignment((short) 0);
		Long elapsed = init - System.currentTimeMillis();
		System.out.println("Backtracking elapsed time:" + JobStatsUtils.timeToMinutes(elapsed));
		assignJobs();
	}
	
	private void mapDeviceswithIDs() {
		deviceQuantity = 0;
		for (Iterator<Device> iterator = devices.values().iterator(); iterator.hasNext();) {
			Device device = (Device) iterator.next();
			deviceIds.put(deviceQuantity, device);
			deviceQuantity++;
		}		
	}

	private void initializeStructures() {
		for (short devNmb = 0; devNmb < deviceQuantity; devNmb++){
			currentAssignment.put(devNmb, new ArrayList<Short>());
			accDataPerDevice.put(devNmb, 0d);
		}
	}

	private void assignJobs() {
		Short devNmb = 0;
		Short[] jobIds;
		for (Iterator<Short> iterator = bestAssignment.keySet().iterator(); iterator.hasNext();) {
			devNmb = (Short) iterator.next();
			jobIds = bestAssignment.get(devNmb);
			sendJobsToDevice(deviceIds.get(devNmb),jobIds);			
		}
	}

	private void sendJobsToDevice(Device device, Short[] jobIds) {		
		for (int jobIndex = 0; jobIndex < jobIds.length; jobIndex++){
			Job job = bufferedJobs.get(jobIndex);
			queueJobTransferring(device, job);
			/*
			Logger.logEntity(this, "Job assigned to ", job.getJobId() ,device);
			long time=NetworkModel.getModel().send(this, device, idSend++,  job.getInputSize(), job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, device, time-currentSimTime,currentSimTime);
			*/
		}	
		
	}

	private void generateBestAssignment(Short nextJob) {		
		if (nextJob == bufferedJobs.size()){
			currentFitnessValue = evaluateCurrentSolution();
			if (currentFitnessValue > bestFitness){
				saveCurrentAssignment();				
			}
		}
		else{
			for (short devNmb = 0; devNmb < deviceQuantity; devNmb++){				
				ArrayList<Short> jobs = currentAssignment.get(devNmb);
				jobs.add(nextJob);
				double jobDataInMb = (((bufferedJobs.get(nextJob).getInputSize()+bufferedJobs.get(nextJob).getOutputSize()) / 1024d ) / 1024d);
				Double accData = accDataPerDevice.get(devNmb);
				accDataPerDevice.put(devNmb, accData+jobDataInMb);
				
				generateBestAssignment((short)(nextJob+1));
				
				int jobcount = jobs.size();				
				if (jobcount > 0) jobs.remove(jobcount-1);
				accDataPerDevice.put(devNmb, accData-jobDataInMb);
			}
		}
	}

	private void saveCurrentAssignment() {
		bestFitness = currentFitnessValue;
		bestAssignment.clear();
		for (Iterator<Short> iterator = currentAssignment.keySet().iterator(); iterator.hasNext();) {
			Short devNmb = (Short) iterator.next();			
			ArrayList<Short> assignedJobs = currentAssignment.get(devNmb);			
			Short[] assignmentsToSave = new Short[assignedJobs.size()];
			assignmentsToSave = assignedJobs.toArray(assignmentsToSave);
			bestAssignment.put(devNmb, assignmentsToSave);
		}	
	}

	private double evaluateCurrentSolution() {
		double totalJoulesConsumed = 0;
		int totalJobTransfered = 0; 
		for (short devNmb=0; devNmb < deviceQuantity; devNmb++){
			 //calculating energy consumed
			 Device device = deviceIds.get(devNmb);
			 double devJoulesConsumed = energyEvaluator.getValue(accDataPerDevice.get(devNmb), device);
			 totalJoulesConsumed+= devJoulesConsumed;
			 
			 //calculating jobs transfered
			 double devicePerOfAvailableEnergy = (double)(device.getLastBatteryLevelUpdate() / DefaultBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION);
			 double deviceJoulesAvailable = ((double)(devicePerOfAvailableEnergy * device.getTotalBatteryCapacityInJoules()) / (double)100);
			 if ((deviceJoulesAvailable - devJoulesConsumed) >= (0d)){
				 totalJobTransfered+=currentAssignment.get(devNmb).size();
			 }
		}
		
		return (((double)totalJobTransfered) / totalJoulesConsumed);
	}
}
