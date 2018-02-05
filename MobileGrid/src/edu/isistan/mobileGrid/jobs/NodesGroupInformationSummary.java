package edu.isistan.mobileGrid.jobs;

import java.util.ArrayList;
import java.util.List;

public class NodesGroupInformationSummary {
	
	private String groupName = "";
	private int nodesQuantity = 0;
	private int sumOfJobsFinishedAndTransfered = 0;
	private int sumOfIncompleteJobs = 0;
	private int sumOfStolenJobs = 0;
	private List<Integer> jobsFinishedAndTranferedList = new ArrayList<Integer>();
	private List<Integer> incompleteJobsList = new ArrayList<Integer>();
	private List<Integer> stolenJobsList = new ArrayList<Integer>();

	public NodesGroupInformationSummary(String name) {
		groupName = name;
	}
	
	public NodesGroupInformationSummary() {		
	}

	public int getNodesQuantity() {
		return nodesQuantity;
	}	
		
	public float getAvgIncompleteJobs() {
		return (float)this.sumOfIncompleteJobs/(float)nodesQuantity;
	}
	
	public float getAvgJobsFinishedAndTransfered(){
		return (float)this.sumOfJobsFinishedAndTransfered/(float)nodesQuantity;
	}
	
	public float getAvgStolenJobs(){
		return (float)this.sumOfStolenJobs/(float)nodesQuantity;
	}
	
	public void addFinishedTransferedJobs(int finishedJobs){
		jobsFinishedAndTranferedList.add(finishedJobs);
		this.sumOfJobsFinishedAndTransfered+=finishedJobs;		
	}
	
	public void addIncompleteJobs(int incompleteJobs){
		incompleteJobsList.add(incompleteJobs);
		this.sumOfIncompleteJobs+=incompleteJobs;
	}
	
	public void addStolenJobs(int stolenJobs){
		stolenJobsList.add(stolenJobs);
		this.sumOfStolenJobs+=stolenJobs;
	}
	
	@Override
	public String toString(){
		String stringRep = "";
		stringRep+=groupName+" Finished and transfered jobs -> total: "+sumOfJobsFinishedAndTransfered+" avg: "+this.getAvgJobsFinishedAndTransfered()+" std: "+getStdJobsFinishedAndTransfered()+"\n";
		stringRep+=groupName+" Stolen jobs -> total: "+sumOfStolenJobs+" avg: "+this.getAvgStolenJobs()+" std: "+getStdStolenJobs()+"\n";
		stringRep+=groupName+" Incomplete jobs -> total:"+sumOfIncompleteJobs+" avg: "+this.getAvgIncompleteJobs()+" std: "+getStdIncompleteJobs()+"\n";
		return stringRep;
	}

	public float getStdStolenJobs() {		
		return getStd(getAvgStolenJobs(),stolenJobsList);
	}

	public float getStdIncompleteJobs() {		
		return getStd(getAvgIncompleteJobs(),incompleteJobsList);
	}

	public float getStdJobsFinishedAndTransfered() {
		return getStd(getAvgJobsFinishedAndTransfered(), jobsFinishedAndTranferedList);		
	}

	private float getStd(float valuesAvg, List<Integer> values) {
		if(valuesAvg==0)return 0.0f;
		
		if (values.size()==this.nodesQuantity){
			double innerSum = 0;
			for(Integer value:jobsFinishedAndTranferedList){
				innerSum+=Math.pow(valuesAvg-value, 2);				
			}
			return (float)Math.sqrt(innerSum/(double)(this.nodesQuantity-1));
		}
		else{
			return Float.NaN;
		}
	}

	public void addNodes(int i) {
		nodesQuantity+=i;		
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
