package edu.isistan.jobs;

public class JobInformation implements Comparable<JobInformation> {

	private long time;
	private int id;
	private long ops;
	private int input;
	private int output;
	
	public JobInformation(long time, int id, long ops, int input, int output) {
		super();
		this.time = time;
		this.id = id;
		this.ops = ops;
		this.input = input;
		this.output = output;
	}


	public long getTime() {
		return this.time;
	}


	public void setTime(long time) {
		this.time = time;
	}


	public int getId() {
		return this.id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public long getOps() {
		return this.ops;
	}


	public void setOps(long ops) {
		this.ops = ops;
	}


	public int getInput() {
		return this.input;
	}


	public void setInput(int input) {
		this.input = input;
	}


	public int getOutput() {
		return this.output;
	}


	public void setOutput(int output) {
		this.output = output;
	}


	@Override
	public String toString() {
		return this.id+";"+this.ops+";"+this.time+";"+this.input+";"+this.output;
	}


	@Override
	public int compareTo(JobInformation o) {
		return (int) (this.time-o.time);
	}

}
