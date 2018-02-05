package edu.isistan.baseprofile;

public class SampleSet {
	
	@Override
	public String toString() {
		return "SampleSet [batterySamples=" + batterySamples + ", cpuSamples="
				+ cpuSamples + "]";
	}
	private String batterySamples;
	private String cpuSamples;
	
	public SampleSet(String battSamples, String cpuSamples){
		this.batterySamples = battSamples;
		this.cpuSamples = cpuSamples;
	}
	
	public String getBatterySamples() {
		return batterySamples;
	}
	public String getCpuSamples() {
		return cpuSamples;
	}
	public void setBatterySamples(String batterySamples) {
		this.batterySamples = batterySamples;
	}
	public void setCpuSamples(String cpuSamples) {
		this.cpuSamples = cpuSamples;
	}  

}
