package edu.isistan.mobileGrid.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.mobileGrid.persistence.IDevicePersister;
import edu.isistan.mobileGrid.persistence.IPersisterFactory;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;
import edu.isistan.mobileGrid.persistence.DBEntity.JobStatsTuple;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Simulation;

import edu.isistan.simulator.Logger;

public class JobStatsUtils {

	private static Map<Job,JobStats> stats=new HashMap<Job, JobStats>();
	private static Map<Node,List<JobStats>> executed=new HashMap<Node, List<JobStats>>();	
	private static Map<Node, NetStats> netStatsPerNode=new HashMap<Node, NetStats>();
	private static HashMap<Node,NodeInformationSummary> nodesInformation = null;
	public static IPersisterFactory persisterFactory;
	public static int sim_id = -1;
	public static long devicesIddleTime = 0;
	public static double totalJobInputData = 0;
	public static double totalJobOutputData = 0;
	
	public static void reset(){
		stats=new HashMap<Job, JobStats>();
	}
	/**
	 * Call when add a new job, if the job is already in the stats it does nothing
	 * @param job
	 * @param node
	 */
	public static void addJob(Job job, Node node){
		if(stats.containsKey(job)) {
			return;
		}

		long time = Simulation.getTime();
		JobStats stat = new JobStatsTuple(job.getJobId(), JobStatsUtils.sim_id, time, node);
		stats.put(job, stat);
	}
	
	
	/**
	 * Set Last Transfer time
	 * @param job
	 * @param time
	 */
	public static void changeLastTransferTime(Job job, long time, long startTime){
		if(!stats.get(job).isSuccess())
			stats.get(job).setLastTransferTime(time, startTime);
		else
			stats.get(job).setLastResultTransferTime(time);
	}
	/**
	 * Call when a job is transfered from a node to another
	 * @param job
	 * @param node
	 * @param time
	 */
	public static void transfer(Job job, Node node, long time, long startTime){
		stats.get(job).addTransfers(node, time, startTime);
	}
	
	public static void setJobTransferCompleted(Job job, Node node) {
		stats.get(job).setJobTransferCompleted(node);
	}
	
	
	/**Call when a device was selected for executing the job*/
	public static void setJobAssigned(Job job) {
		stats.get(job).setAssigned();
	}
	
	/**
	 * Call when a job results are transfered from a node to another
	 * @param job
	 * @param node
	 * @param time
	 */
	public static void transferResults(Job job, Node node, long time){
		stats.get(job).addResultsTransfers(node, time);
	}
	/**
	 * Call when a job starts to be executed
	 * @param job
	 */
	public static void startExecute(Job job){
		JobStats stat = stats.get(job);
		stat.setStartExecutionTime(Simulation.getTime());
		Node n = stat.getTransfers().get(stat.getTransfers().size()-1);
		List<JobStats> l = executed.get(n);
		if (l == null) {
			l = new ArrayList<JobStats>();
			executed.put(n, l);
		}
		l.add(stat);
	}
	/**
	 * Call when a job finished successfully 
	 * @param job
	 */
	public static void success(Job job){		
		JobStats jobStats = stats.get(job); 
		jobStats.setFinishTime(Simulation.getTime());
		jobStats.setExecutedMips(job.getOps());
		jobStats.setSuccess(true);
	}
	
	/**
	 * Call when a job results was successfully transfered back to the proxy or origin node 
	 * @param job
	 */
	public static void successTransferBack(Job job){
		stats.get(job).successTransferedBack();
	}
	
	/**public static void transferBackInitiated(Job job) {
		if(stats.get(job).getTotalExecutionTime()==0) return;
		stats.get(job).transferedBackInitiated();
		
	}*/
	/**
	 * Call when a job failed to finish 
	 * @param job
	 */
	public static void fail(Job job, long executedMips){		
		JobStats jobStats = stats.get(job); 
		jobStats.setFinishTime(Simulation.getTime());
		jobStats.setExecutedMips(executedMips);
		jobStats.setSuccess(false); 
	}
	
	
	/**
	 * Get the total time in the network
	 * @return
	 */
	public static long getTotalTransferTime(){
		long t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalTransferTime();
		return t;
	}
	/**
	 * get the total time of jobs waiting
	 * @return
	 */
	public static long getTotalTime(){
		long t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalTime();
		return t;
	}
	/**
	 * Gets the total time of jobs executing
	 * @return
	 */
	public static long getTotalExecutionTime(){
		long t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalExecutionTime();
		return t;
	}
	/**
	 * Gets the total execution time of jobs that successfully finished executing
	 * @return
	 */
	public static long getEffectiveExecutionTime(){
		long t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).isSuccess())
				t+=stats.get(job).getTotalExecutionTime();
		return t;
	}
	
	public static void printJobStatesSummary(){
		
		int total=0;
		int completed=0;
		int finishedButNotCompleted=0;
		int startedButNotFinished=0;
		int notScheduled=0;
		int queued=0;
		int scheduledButInterrumptedTransfer=0;
		int rejected=0;
		double totalGIP = 0;
		
		for (Job job : stats.keySet()) {
			JobStats js = stats.get(job);
			total++;
			totalGIP+= job.getOps()/(double)(1000000000);
			if (js.isRejected())
				rejected++;
			else{
				 if(!js.isAssigned())				
					 notScheduled++;
				 else{
						if (js.wasReceivedByAWorkerNode()){
							if (js.isCompleted()){
								completed++;
							}
							else{
								if (js.executedSuccessButNotTransferedBack()){
									finishedButNotCompleted++;
								}
								else{
									if(!js.statedToExecute())
										queued++;
									else
										startedButNotFinished++;
								}
							}
						}
						else//These are jobs whose input could not be transfered to
							//the device that were assigned to. Such transfer may or
							//may not start.
							scheduledButInterrumptedTransfer++;
				}
				
			}
			
			
		}
		System.out.println("Total arrived jobs:"+total);
		System.out.println("Rejected:"+rejected);
		System.out.println("Scheduled but transfer interrupted:"+scheduledButInterrumptedTransfer);
		System.out.println("Completed jobs:"+completed);
		System.out.println("FinishedButNotCompleted jobs:"+finishedButNotCompleted);
		System.out.println("StartedButNotFinished jobs:"+startedButNotFinished);
		System.out.println("Queued jobs:"+queued);
		System.out.println("Not scheduled jobs:"+notScheduled);
		System.out.println("Checksum of jobs:"+(rejected+scheduledButInterrumptedTransfer+completed+finishedButNotCompleted+startedButNotFinished+queued+notScheduled));
		
		/*Yisel Log*/
		double sentDataGB = getTotalTransferedData(true)/1024;
		double receivedDataGB = getTotalTransferedData(false)/1024;
		double percentEnergySendingData = getPercentageOfEnergyInSendingData();
		double percentEnergyReceivingData = getPercentageOfEnergyInReceivingData();
		double totalExecutedGIP =  getTotalExecutedGIP();
		double totalDataToTransfer = getAggregatedJobsData(true)+getAggregatedJobsData(false);
		//Logger.logExperiment(total, total-notScheduled-rejected, finishedButNotCompleted + completed, completed, sentDataGB,receivedDataGB, percentEnergySendingData, percentEnergyReceivingData, gips,totalExecutedGIP);
		Logger.logExperiment2(total,
				notScheduled+rejected,
				scheduledButInterrumptedTransfer,
				queued,
				startedButNotFinished,
				finishedButNotCompleted,
				completed,
				sentDataGB,
				receivedDataGB,
				totalDataToTransfer,
				percentEnergySendingData,
				percentEnergyReceivingData,
				totalGIP,
				totalExecutedGIP);
		
		for(Job job:stats.keySet()){
			JobStats js = stats.get(job);
			Logger.logJobDetails(job.getJobId(),js.rejected,js.success,js.successTrasferBack,js.startTime, js.getStartExecutionTime(),js.getFinishTime(), js.getQueueTime(),js.getTotalResultsTransferTime(),js.getTotalTransferTime());
		}
	
		/*double fitness =  (sentDataGB+receivedDataGB)/(totalDataToTransfer) + completed*1.0/total;
		System.out.println("Fitness:"+ fitness);*/
		
	}
	
	/**
	 * Get the number of jobs that were successfully executed
	 * @return
	 */
	public static int getCompletedJobs(){
		int t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).isCompleted())
				t++;
		return t;
	}
	
	/**
	 * Gets the total time of jobs that successfully finished executing
	 * @return
	 */
	public static long getEffectiveTotalTime(){
		long t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).isSuccess())
				t+=stats.get(job).getTotalTime();
		return t;
	}
	/**
	 * Gets the total queue time of jobs that successfully finished executing
	 * @return
	 */
	public static long getEffectiveQueueTime(){
		long t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).isSuccess())
				t+=stats.get(job).getQueueTime();
		return t;
	}
	/**
	 * Gets the total queue time of all the jobs
	 * @return
	 */
	public static long getTotalQueueTime(){
		long t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getQueueTime();
		return t;
	}
	/**
	 * Get the number of jobs that started to execute 
	 * @return
	 */
	public static int getNumberOfJobsStarted(){
		int t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).statedToExecute())
				t++;
		return t;
	}
	/**
	 * Get the number of jobs 
	 * @return
	 */
	public static int getSize(){
		return stats.size();
	}
	/**
	 * Get the number of jobs that were successfully executed
	 * @return
	 */
	public static int getSuccessfullyExecutedJobs(){
		int t=0;
		for(Job job:stats.keySet())
			if(stats.get(job).isSuccess())
				t++;
		return t;
	}
	
	/**
	 * Get registered Jobs
	 * @return
	 */
	public static Iterator<Job> getJob(){
		return stats.keySet().iterator();
	}
	/**
	 * Return the stats for a Job
	 * @param j
	 * @return
	 */
	public static JobStats getJobStats(Job j){
		return stats.get(j);
	}
	
	public static String timeToMinutes(long milis){
		StringBuffer sb=new StringBuffer();
		double aux=Math.floor(milis/60000d);
		sb.append((long)aux);
		sb.append(":");
		aux=(milis-aux*60000)/1000d;
		sb.append(aux);
		return sb.toString();
	}
	
	public static String timeToHours(long milis){
		StringBuffer sb=new StringBuffer();
		int h = (int) Math.floor(milis/(60*60*1000d));
		int m = (int) Math.floor(milis/(60*1000d));
		int s = (int) Math.floor(milis/1000d);
		int ms = (int) (milis-s*1000);
		s = s-m*60;
		m = m-h*60;
		sb.append(h);
		sb.append(':');
		sb.append(m);
		sb.append(':');
		sb.append(s);
		sb.append('.');
		sb.append(ms);
		return sb.toString();
	}
	public static long getTotalResultsTransferTime() {
		long t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalResultsTransferTime();
		return t;
	}

	public static int cantJobTrasnfers(){		
		int t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalTransfers();
		return t;
	}
	
	public static int cantJobSuccessButNotTransfered(){
		int t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).executedSuccessButNotTransferedBack()? 1 : 0;
		return t;
	}
	
	public static int cantJobResultTransfers(){		
		int t=0;
		for(Job job:stats.keySet())
			t+=stats.get(job).getTotalResultTransfers();
		return t;
	}
	
	public static Set<Job> getJobs(){
		return stats.keySet();
	}
	
	public static List<JobStats> getJobStats(){
		List<JobStats> jobs = new ArrayList<JobStats>(stats.size());
		for(Job j:stats.keySet())
			jobs.add(stats.get(j));
		return jobs;
	}
	
	public static List<JobStats> getJobStatsExecutedIn(Node n){
		List<JobStats> l=executed.get(n);
		if(l == null ) {
			l = new ArrayList<JobStats>();
			executed.put(n, l);
		}
		return l;
	}
	
	/**this method allows to register an amount of energy wasted in sending data for a given node. The energy in sending data spent by the node increases with every call to this method.*/
	public static boolean registerSendingDataEnergy(Node node, double joulesInSendingData, double megabytesOfSentdData) {
		NetStats energyCosts = netStatsPerNode.get(node);
		if (energyCosts != null){
			energyCosts.addJoulesInSendingData(joulesInSendingData);
			energyCosts.addMegabytesSent(megabytesOfSentdData);
			return energyCosts.IsMaximumAvailableJoulesExcedeed();
		}
		else{			 
			double maxJoulesOfDevice = Double.MAX_VALUE;
			
			if (node.runsOnBattery()){
				Device d = ((Device)node);
				maxJoulesOfDevice  = (((double)d.getInitialSOC()/(double)BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)* (double)d.getTotalBatteryCapacityInJoules())/(double)100;
			}
			energyCosts = new NetStats(maxJoulesOfDevice,joulesInSendingData,0.0d,megabytesOfSentdData,0.0d);
			netStatsPerNode.put(node, energyCosts);
			return energyCosts.IsMaximumAvailableJoulesExcedeed();
		}		
	}

	/**this method allows to register an amount of energy wasted and amount of data Received for a given node.
	 * The energy and received data is added to the previously registered values of the corresponding node.*/
	public static boolean registerReceivingDataEnergy(Node node, double joulesInReceivingData, double megabytesOfReceivedData) {
		NetStats energyCosts = netStatsPerNode.get(node);
		if (energyCosts != null){			
			energyCosts.addMegabytesReceived(megabytesOfReceivedData);
			energyCosts.addJoulesInReceivingData(joulesInReceivingData);
			return energyCosts.IsMaximumAvailableJoulesExcedeed();
		}
		else{
			double maxJoulesOfDevice = Double.MAX_VALUE;
		
			if (node.runsOnBattery()){
				Device d = ((Device)node);
				maxJoulesOfDevice = (((double)d.getInitialSOC()/(double)BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)* (double)d.getTotalBatteryCapacityInJoules())/(double)100;
			}
			
			energyCosts = new NetStats(maxJoulesOfDevice,0.0d,joulesInReceivingData,0.0d,megabytesOfReceivedData);
			netStatsPerNode.put(node, energyCosts);
			return energyCosts.IsMaximumAvailableJoulesExcedeed();
		}
		
	}
	
	public static double getTotalEnergyInDataTransmision() {
		double totalEnergyInDataTransmision = 0;
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				NetStats energyCost = netStatsPerNode.get(device);
				totalEnergyInDataTransmision+=energyCost.getAccJoulesInReceivingData();
				totalEnergyInDataTransmision+=energyCost.getAccJoulesInSendingData();
			}
		}
		return totalEnergyInDataTransmision;
	}
	
	public static double getPercentageOfEnergyInDataTransmision() {
		return (getTotalEnergyInDataTransmision() * (double)100) / getGridBatteryPoweredEnergy();
	}
	
	public static void registerUpdateMessage(Node scr, UpdateMsg data) {
		NetStats netStats = netStatsPerNode.get(scr);
		if (netStats != null){
			netStats.setUpdateMsgCount(netStats.getUpdateMsgCount()+1);
		}
		else{
			netStats = new NetStats();
			netStats.setUpdateMsgCount(netStats.getUpdateMsgCount()+1);
			netStatsPerNode.put(scr, netStats);
		}
	}
	
	/**returns the total executed job instructions expressed in billion of instructions (GIP)*/
	public static double getTotalExecutedGIP(){
		List<JobStats> jobStats = getJobStats();
		double executedInstruccions = 0.0d;
		for (Iterator<JobStats> iterator = jobStats.iterator(); iterator.hasNext();) {
			JobStats jobStat = (JobStats) iterator.next();
			if (!jobStat.isRejected() || !jobStat.isSuccess())
				executedInstruccions += ((double)jobStat.getExecutedMips())/(double)(1000000000);
		}
		return executedInstruccions;
	}
	
	public static int getTotalUpdateMsgSentByNodes(){
		int totalUpdatemsg = 0;
		for (Node node : netStatsPerNode.keySet()) {
			if (node.runsOnBattery()){
				totalUpdatemsg+=netStatsPerNode.get(node).getUpdateMsgCount();
			}
		}
		return totalUpdatemsg;
	}
	
	public static int getTotalUpdateMsgReceivedByProxy(){
		NetStats proxyStats = netStatsPerNode.get(SchedulerProxy.PROXY);
		return proxyStats!=null ? proxyStats.getUpdateMsgCount() : 0;		
	}
	
	public static double getGridBatteryPoweredEnergy(){
		double gridTotalEnergy = 0;
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				Device dev = (Device)device;
				gridTotalEnergy+=(((dev.getInitialSOC() / BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * dev.getTotalBatteryCapacityInJoules()) / (double)100);
			}
		}
		return gridTotalEnergy;
	}
	
	public static double getPercentageOfEnergyInSendingData() {
		return (getTotalEnergyInSendingData() * (double)100)/getGridBatteryPoweredEnergy();
	}
	
	public static double getTotalEnergyInSendingData() {
		double totalEnergyInDataTransmision = 0;
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				NetStats energyCost = netStatsPerNode.get(device);
				totalEnergyInDataTransmision+=energyCost.getAccJoulesInSendingData();
			}
		}
		return totalEnergyInDataTransmision;
	}
	
	public static double getPercentageOfEnergyInReceivingData() {
		return (getTotalEnergyInReceivingData() * (double)100) / getGridBatteryPoweredEnergy();
	}
	
	public static double getTotalEnergyInReceivingData() {
		double totalEnergyInDataTransmision = 0;
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				NetStats energyCost = netStatsPerNode.get(device);
				totalEnergyInDataTransmision+=energyCost.getAccJoulesInReceivingData();
			}
		}
		return totalEnergyInDataTransmision;
	}
	
	public static double getTotalTransferedData(boolean sent) {
		double totalData = 0;
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				NetStats energyCost = netStatsPerNode.get(device);				
				if (sent)
					totalData+=energyCost.getAccMegabytesSent();
				else
					totalData+=energyCost.getAccMegabytesReceived();
			}
		}
		return totalData;
	}
	
	public static double getPercentOfTransferedData()
	{
		double sentData = JobStatsUtils.getTotalTransferedData(true)/1024;
		double receivedData = JobStatsUtils.getTotalTransferedData(false)/1024;
		double totalData =  getAggregatedJobsData(true) + getAggregatedJobsData(false);
		return (sentData+receivedData)*100/totalData;
	}
	
	/**this method returns the data input represented in gb by all jobs that arrived to the grid.
	 * With dataInput in true the aggregated job data input is returned while when it is in false
	 * the aggregated job data output is returned.*/
	public static double getAggregatedJobsData(boolean dataInput){
		double totalJobsData = 0d;
		for (Job job : stats.keySet()) {
			totalJobsData += dataInput? job.getInputSize() : job.getOutputSize();			
		}
		return totalJobsData/(1024*1024*1024);
	}
	
	private static void generateNodeInformationSummary(){				
		if (nodesInformation == null)
			nodesInformation = new HashMap<Node,NodeInformationSummary>();
		
		Collection<JobStats> jobs = stats.values();
		for (Iterator<JobStats> iterator = jobs.iterator(); iterator.hasNext();) {
			JobStats jobStats = iterator.next();
			List<Node> transfers = jobStats.getTransfers(); 
			Node lastNode = transfers.get(transfers.size()-1);
			if (((Entity)lastNode).getName().compareTo("PROXY")!=0){
				NodeInformationSummary lastNodeInfo = getNodeInfoSummary(lastNode);

				if(jobStats.isCompleted()) lastNodeInfo.setFinishedAndTransferedJobs(lastNodeInfo.getFinishedAndTransferedJobs()+1);
				if(!jobStats.isCompleted()) lastNodeInfo.setIncompleteJobs(lastNodeInfo.getIncompleteJobs()+1);
				if(!jobStats.statedToExecute()) lastNodeInfo.setNotStartedJobs(lastNodeInfo.getNotStartedJobs()+1);
				lastNodeInfo.setJobExecutionTime(lastNodeInfo.getJobExecutionTime()+jobStats.getTotalExecutionTime());

				if(transfers.size() == 3)//if the last node stole the job to the first
					lastNodeInfo.setStolenJobs(lastNodeInfo.getStolenJobs()+1);
				else{
					if (transfers.size() > 3){
						for (int i=2; i < transfers.size(); i++) {//i starts in 2 because the transfer between the proxy and the first node does not count as a steal
							NodeInformationSummary nodeInfo = getNodeInfoSummary(transfers.get(i));
							nodeInfo.setStolenJobs(nodeInfo.getStolenJobs()+1);
						}
					}
				}
			
			}
		}
	}
		
	public static void printNodeInformationSummary(){
		if (nodesInformation == null)			
			generateNodeInformationSummary();		
		System.out.println("--------------Node information summaries-----------------");
		Set<Node> nodes = nodesInformation.keySet();
		Iterator<Node> nodesIterator = nodes.iterator(); 
		while (nodesIterator.hasNext()){
			Node node = nodesIterator.next();
			NodeInformationSummary nodeInfo = nodesInformation.get(node);
			System.out.println("/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*");
			System.out.println("Node type: "+ ((Device)node).getMIPS());
			System.out.print(nodeInfo.toString());
		}
		System.out.println("------------------------------------------");
	}		
	
	private static NodeInformationSummary getNodeInfoSummary(Node node) {
		if (!nodesInformation.containsKey(node))
			nodesInformation.put(node, new NodeInformationSummary(((Device)node).getName(),((Device)node).getMIPS()));
		return nodesInformation.get(node);		
	}
	
	
	public static void printNodeInformationSummaryByNodeMips() {
		if (nodesInformation == null)			
			generateNodeInformationSummary();
		
		HashMap<Long,NodesGroupInformationSummary> groupsInfo = new HashMap<Long,NodesGroupInformationSummary>(); 
		
		for (Iterator<NodeInformationSummary> iterator = nodesInformation.values().iterator(); iterator.hasNext();) {
			NodeInformationSummary nodeInfo = iterator.next();
			
			if(!groupsInfo.containsKey(nodeInfo.getMips())){
				groupsInfo.put(nodeInfo.getMips(), new NodesGroupInformationSummary(String.valueOf(nodeInfo.getMips())));
			}
			
			NodesGroupInformationSummary nodesGroupInfo = groupsInfo.get(nodeInfo.getMips());
			nodesGroupInfo.addFinishedTransferedJobs(nodeInfo.getFinishedAndTransferedJobs());
			nodesGroupInfo.addStolenJobs(nodeInfo.getStolenJobs());
			nodesGroupInfo.addIncompleteJobs(nodeInfo.getIncompleteJobs());
			nodesGroupInfo.addNodes(1);
		}
		
		System.out.println("--------------Node information groups summaries-----------------");
		Set<Long> nodes = groupsInfo.keySet();
		Iterator<Long> nodesIterator = nodes.iterator(); 
		while (nodesIterator.hasNext()){
			Long nodeGroupMips = nodesIterator.next();
			NodesGroupInformationSummary nodesGroupInfo = groupsInfo.get(nodeGroupMips);
			System.out.println("************************************");
			System.out.println("Node mips: "+ nodeGroupMips);
			System.out.print(nodesGroupInfo.toString());
		}
		System.out.println("----------------------------------------------------------------");
		
	}
	public static void storeInDB() {
		IDevicePersister devicePersister = persisterFactory.getDevicePersister();
		SQLSession session = devicePersister.openSQLSession();
		
		devicePersister.insertInMemoryDeviceTuples(session);
		session.commit();
		
		Collection<JobStats> jobs = stats.values();
		for (Iterator<JobStats> iterator = jobs.iterator(); iterator.hasNext();) {
			JobStatsTuple jobStats = (JobStatsTuple)iterator.next();
			jobStats.persist(session);
		}
		session.commit();
		session.close();		
	}
	
	
	public static void deviceJoinTopology(Device device, long startTime) {
		DeviceTuple dt = persisterFactory.getDevicePersister().getDevice(((Entity)device).getName());
		if (dt != null)
			dt.setJoin_topology_time(startTime);		
	}
	
	
	public static void deviceLeftTopology(Device device, long leftTime) {
		DeviceTuple dt = persisterFactory.getDevicePersister().getDevice(((Entity)device).getName());
		if (dt != null)
			dt.setLeft_topology_time(leftTime);
	}
	public static int getSim_id() {
		return sim_id;
	}
	public static void setSim_id(int sim_id) {
		JobStatsUtils.sim_id = sim_id;
	}
	public static void incIddleTime(long l) {
		devicesIddleTime+=l;		
	}
	
	/**Remove comment for testing purposes*/
	public static String printNodesPercentageOfEnergyWasteInNetworkActivity() {
		for (Node device : netStatsPerNode.keySet()) {
			if (device != null && device.runsOnBattery()){
				Device dev = (Device)device;
				System.out.println("Device "+dev.getName()+" "+ dev.getEnergyPercentageWastedInNetworkActivity());
			}
		}
		return null;
	}
	public static int getTotalStealings() {
		if (nodesInformation == null)			
			generateNodeInformationSummary();
		int stealings=0;
		
		for (Iterator<NodeInformationSummary> iterator = nodesInformation.values().iterator(); iterator.hasNext();) {
			NodeInformationSummary nodeInfo = (NodeInformationSummary) iterator.next();
			stealings+=nodeInfo.getStolenJobs();
		}
		return stealings;
	}
	
	/**The method is invoked when a job is left out of the scheduling because
	 * there are not devices able to handle the job.*/
	public static void rejectJob(Job job, long rejectTime) {
		JobStats js = stats.get(job);
		js.setRejected(true);
		js.setExecutedMips(0);
		js.setFinishTime(rejectTime);
	}
	
	
}
