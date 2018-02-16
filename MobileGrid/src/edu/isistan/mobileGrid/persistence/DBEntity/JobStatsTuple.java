package edu.isistan.mobileGrid.persistence.DBEntity;

import java.sql.SQLException;

import edu.isistan.mobileGrid.jobs.JobStats;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.persistence.IDevicePersister;
import edu.isistan.mobileGrid.persistence.IJobStatsPersister;
import edu.isistan.mobileGrid.persistence.IJobTransferedPersister;
import edu.isistan.mobileGrid.persistence.IPersisterFactory;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.simulator.Entity;

public class JobStatsTuple extends JobStats {
	
	private static IJobStatsPersister jsp = null;
	private static IJobTransferedPersister jtp = null;
	private static IDevicePersister dp = null;
	private int job_id;
	private int jobstats_id;
	private int sim_id;
	
	public JobStatsTuple(int job_id, int sim_id, long transferTime, Node node) {
		super(transferTime, node);
		this.job_id = job_id;
		this.sim_id = sim_id;
		this.jobstats_id = -1;
	}
	
	public static void setIPersisterFactory(IPersisterFactory pf){		
		jtp = pf.getJobTransferedPersister();
		jsp = pf.getJobStatsPersister();
		dp = pf.getDevicePersister();
	}
	
	public void persist(SQLSession session){
		
		insertJobStats(session,this);		
		Node firstNode = transfersInfo.get(0).getNode();
		boolean lastHop = transfersInfo.size()==1 ? true : false;//in other words if there is only one transfer for the job then this transfer is the last hop of the chain of transfers
				
		if(!(firstNode instanceof Device)){
			insertJobTransfer(session,this,null, (Entity)firstNode, 0,0,startTime,lastHop);//the transfer time between the an out of the grid device and the proxy does not matter so for that reason is cero.  
		}
		
		for (int i = 1; i < transfersInfo.size() - 1; i++){
			Entity originNode = (Entity)transfersInfo.get(i-1).getNode();
			Entity destNode = (Entity)transfersInfo.get(i).getNode();
			insertJobTransfer(session,this, (Entity)originNode, (Entity)destNode, transfersInfo.get(i-1).getTransferTime(),
					i, transfersInfo.get(i-1).getStartTransferTime(), lastHop);
		}
		
		if(!lastHop){
			lastHop = true;
			Entity originNode = (Entity)transfersInfo.get(transfersInfo.size()-2).getNode();
			Entity destNode = (Entity)transfersInfo.get(transfersInfo.size()-1).getNode();
			insertJobTransfer(session,this, (Entity)originNode, (Entity)destNode, transfersInfo.get(transfersInfo.size()-1).getTransferTime(),
                    transfersInfo.size()-1, transfersInfo.get(transfersInfo.size()-1).getStartTransferTime(), lastHop);
		}
	}
	
	private static void insertJobStats(SQLSession session, JobStatsTuple stat) {
				 
		try {			
			jsp.insertJobStats(session, stat);
		} catch (SQLException e) {			
			e.printStackTrace();
		}				
	}
	
	private static void insertJobTransfer(SQLSession session, JobStatsTuple stat, Entity originNode, Entity destNode, long time, int hop, long startTime, boolean lastHop) {
		Integer from_deviceId = null;
		if (originNode != null) 
			from_deviceId = dp.getDevice(originNode.getName()).getDevice_id();
		Integer to_deviceId = dp.getDevice(destNode.getName()).getDevice_id(); 		
		JobTransfer jt = new JobTransfer(stat.getJobstats_id(),from_deviceId,to_deviceId,time,hop, startTime,lastHop);
		try {			
			jtp.insertJobTransfered(session, jt);			
		} catch (SQLException e) {			
			e.printStackTrace();
		}				
	}	
	
	public boolean isSuccessTransferBack(){
		return this.successTrasferBack;
	}

	public int getJob_id() {
		return job_id;
	}

	public void setJob_id(int job_id) {
		this.job_id = job_id;
	}

	public int getSim_id() {
		return sim_id;
	}

	public void setSim_id(int sim_id) {
		this.sim_id = sim_id;
	}

	public int getJobstats_id() {
		return jobstats_id;
	}

	public void setJobstats_id(int jobstats_id) {
		this.jobstats_id = jobstats_id;
	}
	
	public int getLastTransferedNode(){
		Entity executorNode = (Entity)transfersInfo.get(transfersInfo.size()-1).getNode();
		return dp.getDevice(executorNode.getName()).getDevice_id();	
	}

}
