package edu.isistan.mobileGrid.persistence.DBEntity;

import java.sql.Timestamp;


public class SimulationTuple {

	private int sim_id = -1;
	private String name = "";
	private String	scheduler = "";
	private String	comparator = "";
	private String	policy= "";
	private String	strategy= "";
	private String	condition= "";
	private String	link= "";	     					
	private String	topology_file= "";
	private String	base_profile= "";
	private String	jobs_file= "";
	private Timestamp	start_time = null;
	
	public SimulationTuple() {
		
	}

	public int getSim_id() {
		return sim_id;
	}

	public void setSim_id(int sim_id) {
		this.sim_id = sim_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScheduler() {
		return scheduler;
	}

	public void setScheduler(String scheduler) {
		this.scheduler = scheduler;
	}

	public String getComparator() {
		return comparator;
	}

	public void setComparator(String comparator) {
		this.comparator = comparator;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getTopology_file() {
		return topology_file;
	}

	public void setTopology_file(String topology_file) {
		this.topology_file = topology_file;
	}

	public String getBase_profile() {
		return base_profile;
	}

	public void setBase_profile(String base_profile) {
		this.base_profile = base_profile;
	}

	public String getJobs_file() {
		return jobs_file;
	}

	public void setJobs_file(String jobs_file) {
		this.jobs_file = jobs_file;
	}

	public Timestamp getStart_time() {
		return start_time;
	}

	public void setStart_time(Timestamp start_time) {
		this.start_time = start_time;
	}
	

}
