package edu.isistan.mobileGrid.jobs;

import java.util.ArrayList;
import java.util.List;

import edu.isistan.mobileGrid.network.Node;

public class JobStats{	
	
	protected boolean assigned = false;
	protected boolean success=false;
	protected boolean successTrasferBack=false;
	protected boolean rejected=false;
	//private boolean transferBackInitiated = false;
	
	/**this list indicates whether a job transferring was completed or not, i.e., received by the destiny node*/
	protected List<Boolean> transfersCompleted=new ArrayList<Boolean>();	
	protected List<Long> startTransferTimes=new ArrayList<Long>();
	protected List<Node> transfers=new ArrayList<Node>();	
	protected List<Long> transferTimes=new ArrayList<Long>();	
	
	private List<Node> resultsTransfers=new ArrayList<Node>();
	private List<Long> resultsTransfersTimes=new ArrayList<Long>();
	
	//this time is set when the job enters the mobile grid
	protected long startTime=-1;
	
	//this value represent the executed mips of the job and is set at the same time the finishTime is set
	protected long executedMips=0L;
	
	//this time is set when the job starts to be executed by a node
	protected long startExecutionTime=-1;
	
	//this time is set when the job finishes normally or abnormally. It remains in -1 value only when the job execution time was never set 
	protected long finishTime=-1;
		
	
	public JobStats(long startTime, Node node) {
		super();
		this.startTime = startTime;
		this.transfersCompleted.add(true);
		this.transfers.add(node);
		this.transferTimes.add(startTime);
		this.startTransferTimes.add(startTime);
	}
	
	public boolean isAssigned() {
		return assigned;
	}
	
	public void setAssigned() {
		assigned =  true;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public boolean isCompleted(){
		return success&&successTrasferBack;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public List<Node> getTransfers() {
		return transfers;
	}
	
	
	public void addTransfers(Node node, long time, long startTime) {
		this.transfersCompleted.add(false);
		this.transfers.add(node);
		this.transferTimes.add(time);
		this.startTransferTimes.add(startTime);
	}
	
	public void addResultsTransfers(Node node, long time) {
		this.resultsTransfers.add(node);
		this.resultsTransfersTimes.add(time);
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public void setLastTransferTime(long time, long startTime){
		this.transferTimes.remove(this.transferTimes.size()-1);
		this.transferTimes.add(time);
		this.startTransferTimes.remove(this.startTransferTimes.size()-1);
		this.startTransferTimes.add(startTime);
	}
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getStartExecutionTime() {
		return startExecutionTime;
	}
	
	public void setStartExecutionTime(long startExecutionTime) {
		this.startExecutionTime = startExecutionTime;
	}
	
	public long getFinishTime() {
		return finishTime;
	}
	
	public void setFinishTime(long finishTime) {
		this.finishTime = finishTime;
	}
	
	public List<Long> getTransferTimes() {
		return transferTimes;
	}
	
	public long getTotalTransferTime(){
		long t=0;
		for(Long l:this.transferTimes)
			t+=l;
		return t;
	}
	
	public long getTotalResultsTransferTime(){
		long t=0;
		for(Long l:this.resultsTransfersTimes)
			t+=l;
		return t;
	}
	
	public long getTotalTime(){
		return this.finishTime-this.startTime;
	}
	
	public long getTotalExecutionTime(){
		if(this.startExecutionTime==-1) return -1;
		return this.finishTime-this.startExecutionTime;//be carefull with this value, very small jobs may have 0 milliseconds of execution time
	}
	
	public boolean statedToExecute(){
		return this.startExecutionTime!=-1;
	}

	public long getQueueTime() {
		long result=0;
		if(this.isSuccess())
			result=this.startExecutionTime-this.startTime;
		else
			result=this.finishTime-this.startTime;
		return result;
	}
	
	public void successTransferedBack(){
		this.successTrasferBack = true;
	}

	public void setLastResultTransferTime(long time) {
		this.resultsTransfersTimes.remove(this.transferTimes.size()-1);
		this.resultsTransfersTimes.add(time);
	}

	public int getTotalTransfers() {
		return this.transferTimes.size();
	}
	
	public int getTotalResultTransfers() {
		return this.resultsTransfers.size();
	}
	
	public boolean executedSuccessButNotTransferedBack(){
		return (this.success && !this.successTrasferBack);
	}

	/**public void transferedBackInitiated() {
		this.transferBackInitiated =true;		
	}

	public boolean isTransferBackInitiated() {
		return transferBackInitiated;
	}*/

	public void setJobTransferCompleted(Node node) {
		/**@TODO: in the case the job is sent several times to the same node, this method will set as complete the first transferring. Provide a fix
		 * to this behavior if this case becomes usual in simulations
		 * */ 
		int jobPosition = transfers.indexOf(node);		
		this.transfersCompleted.remove(jobPosition);
		this.transfersCompleted.add(jobPosition, true);		
	}

	public boolean wasReceivedByAWorkerNode() {
		//first node of the transfer node list is ignored because it is considered that is the node from which de job was scheduled 
		for (int i=1; i < this.transfersCompleted.size(); i++) {			
			if (this.transfersCompleted.get(i).booleanValue()==true) return true;
		}
		return false;
	}

	public boolean isRejected() {
		return rejected;
	}

	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	public long getExecutedMips() {
		return executedMips;
	}

	public void setExecutedMips(long executedMips) {
		this.executedMips = executedMips;
	}
	

}