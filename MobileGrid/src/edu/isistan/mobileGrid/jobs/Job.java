package edu.isistan.mobileGrid.jobs;

public class Job {
	
	private int jobId;
	private long ops;
	private int src;
	private int inputSize;
	private int outputSize;
	
	public Job(int jobId, long ops, int src, int inputSize, int outputSize) {
		super();
		this.jobId = jobId;
		this.ops = ops;
		this.src = src;
		this.inputSize = inputSize;
		this.outputSize = outputSize;
	}

	public long getOps() {
		return ops;
	}

	public void setOps(long ops) {
		this.ops = ops;
	}

	public int getSrc() {
		return src;
	}

	public void setSrc(int src) {
		this.src = src;
	}

	public int getInputSize() {
		return inputSize;
	}

	public void setInputSize(int inputSize) {
		this.inputSize = inputSize;
	}

	public int getOutputSize() {
		return outputSize;
	}

	public void setOutputSize(int outputSize) {
		this.outputSize = outputSize;
	}

	public int getJobId() {
		return jobId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Job){
			return ((Job)obj).jobId==this.jobId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.jobId;
	}

	@Override
	public String toString() {
		return "Job [jobId=" + this.jobId + "]";
	}
	

}
