package edu.isistan.mobileGrid.persistence.DBEntity;

public class JobTransfer {
	
	private Integer jobtransfered_id;
	private Integer	jobstats_id;
	private Integer	from_device_id;
	private Integer	to_device_id;
	private Integer	hop;	
	private Long	time;
	private Long	startTime;
	private boolean lastHop;
	
	public JobTransfer(Integer jobstats_id, Integer from_device_id, Integer to_device_id, Long time, Integer hop, Long startTime, boolean lastHop){
		this.jobstats_id = jobstats_id;
		this.setFrom_device_id(from_device_id);
		this.to_device_id = to_device_id;
		this.time = time;
		this.hop = hop;
		this.setStartTime(startTime);
		this.lastHop = lastHop;
	}

	
	public Integer getJobstats_id() {
		return jobstats_id;
	}

	public boolean isLastHop(){
		return lastHop;
	}

	public void setJobstats_id(Integer jobstats_id) {
		this.jobstats_id = jobstats_id;
	}


	public Integer getTo_device_id() {
		return to_device_id;
	}


	/**@param device_id
	 * the id of device where the job is transfered to*/
	public void setTo_device_id(Integer device_id) {
		this.to_device_id = device_id;
	}


	public Long getTime() {
		return time;
	}


	public void setTime(Long time) {
		this.time = time;
	}


	public Integer getHop() {
		return this.hop;
	}


	public void setHop(Integer hop) {
		this.hop = hop;
	}

	public Integer getJobtransfered_id() {
		return jobtransfered_id;
	}


	public void setJobtransfered_id(Integer jobtransfered_id) {
		this.jobtransfered_id = jobtransfered_id;
	}


	public Long getStartTime() {
		return startTime;
	}


	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}


	public Integer getFrom_device_id() {
		return from_device_id;
	}


	public void setFrom_device_id(Integer from_device_id) {
		this.from_device_id = from_device_id;
	}

}
