package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.seas.proxy.DataAssignment;

public class GeneticAssignmentRound {

	//data needed to start a scheduling genetic round
	private ArrayList<Job> jobsToSchedule;
	private double totalDataToBeSchedule;
	
	
	//data generated after the scheduling genetic round
	private ArrayList<DataAssignment> assignment;
	
	//information data of the current scheduling genetic round	
	private GAExecInformation gaInfo;
	private long assignmentStartTime;
	private long assignmentFinishedTime;
	
	public GeneticAssignmentRound(ArrayList<Job> jobsToSchedule, double totalJobDataToBeSchedule){
		this.jobsToSchedule = jobsToSchedule;
		this.totalDataToBeSchedule = totalJobDataToBeSchedule;		
	}
	
	public ArrayList<DataAssignment> getAssignment() {
		return assignment;
	}

	public void setAssignment(ArrayList<DataAssignment> assignment) {
		this.assignment = assignment;
	}

	public GAExecInformation getGaInfo() {
		return gaInfo;
	}

	public void setGaInfo(GAExecInformation gaInfo) {
		this.gaInfo = gaInfo;
	}

	public long getAssignmentFinishedTime() {
		return assignmentFinishedTime;
	}

	public ArrayList<Job> getJobsToSchedule() {
		return jobsToSchedule;
	}

	public double getTotalDataToBeSchedule() {
		return totalDataToBeSchedule;
	}

	public long getAssignmentStartTime() {
		return assignmentStartTime;
	}

	public Job getJob(int job) {
		if (jobsToSchedule != null && job < jobsToSchedule.size())
				return (Job)jobsToSchedule.get(job);
		
		return null;
	}

	public double getGenesAmount() {		
		return jobsToSchedule.size();
	}

	public void setAssignmentStartTime(long assignmentStartTime) {
		this.assignmentStartTime = assignmentStartTime;
	}

	public void setAssignmentFinishedTime(long assignmentFinishedTime) {
		this.assignmentFinishedTime = assignmentFinishedTime;
	}
	
	
}
