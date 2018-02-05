package edu.isistan.baseprofile;

public interface ProfileReader {
	
	public void readSample();
	
	public void closeReader();
	
	public long getCurrentSampleTime();
	
	public long getPreviousSampleTime();

	public String currentSample();
	
	public String previousSample(); 

}
