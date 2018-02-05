package edu.isistan.baseprofile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class CpuProfileReader implements ProfileReader{

	private static final int FIRST_SAMPLE = 0;
	private BufferedReader cpuProfile;	
	private String currentCpuSample = null;
	private String previousCpuSample = null;	
	private String profilename;
	
	//the next two variables are used to avoid extra sample readings that can results from invocations
	//of the readUntilSampleWithTime() method. 
	private String antepenultimateCpuSample=null;
	private boolean usePreviousReading = false;
	
	/**Given a cpu sample, this method returns its time.
	 * The sample should respect the the format:
	 * event_name;time*/
	public static String getSampleTime(String sample){
		StringTokenizer st = new StringTokenizer(sample,";");
		st.nextToken();
		return st.nextToken();
	}
	
	
	public CpuProfileReader(String file){
		try {
			
			profilename=file.substring(file.lastIndexOf("cpu"), file.lastIndexOf(".cnf"));			
			cpuProfile=new BufferedReader(new FileReader(new File(file)));
			readSample();			
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		
	}	
	
	/**Return the next line of the profile. The method avoids returning the empty lines.*/
	public void readSample(){
		if(!usePreviousReading){		
		
			try {
				antepenultimateCpuSample = previousCpuSample;
				previousCpuSample = currentCpuSample;
				currentCpuSample=cpuProfile.readLine();
			
				while (currentCpuSample != null && currentCpuSample.trim().equals("")){					
						currentCpuSample=cpuProfile.readLine();
				}					
			} catch (IOException e) {
				e.printStackTrace();			
			}
		}
		else{
			usePreviousReading=false;
		}
	}
	
	public long getCurrentSampleTime(){		
		if (currentCpuSample!= null){
			if (!usePreviousReading){
				StringTokenizer st=new StringTokenizer(currentCpuSample, ";");
				st.nextToken();
				return Long.parseLong(st.nextToken());
			}
			else{
				if (previousCpuSample != null){
					StringTokenizer st=new StringTokenizer(previousCpuSample, ";");
					st.nextToken();
					return Long.parseLong(st.nextToken());
				}
				else{
					//System.out.println("WARN: current cpu sample time is -1 because cpu sample is null");
					return -1;
				}
			}
		}
		else{
			//System.out.println("WARN: current cpu sample time is -1 because current cpu sample is null");
			return -1;
		}
	}
	
	public long getPreviousSampleTime(){
		if (previousCpuSample!= null){
			if (!usePreviousReading){
				StringTokenizer st=new StringTokenizer(previousCpuSample, ";");
				st.nextToken();
				return Long.parseLong(st.nextToken());
			}
			else{
				if (antepenultimateCpuSample!=null){
					StringTokenizer st=new StringTokenizer(antepenultimateCpuSample, ";");
					st.nextToken();
					return Long.parseLong(st.nextToken());
				}
				else
					return getCurrentSampleTime();
			}
		}
		else{
			return getCurrentSampleTime();
		}
	}	
	

	/**Leaving from the currentSample, this method read samples of the profile until finding the first one that happens at a time equal or greater than the time passed as argument
	 * The profile is assumed to be ordered by time.
	 * A null value could be returned if the time is not part of the profile*/
	public String readUntilSampleWithTime(long time) {		
		while (currentCpuSample != null && this.getCurrentSampleTime() < time)
			this.readSample();		 
		if (currentCpuSample != null && this.getCurrentSampleTime() == time)
			return currentCpuSample;
		else
			//an extra reading was made so the method should return the previous sample. Then,
			//the previous sample represent the current sample that the profile reader should return in a new invocation of			
			//the readSample() method. For this reason the antipenultimateCpuSample and the usePreviousReading variables 
			//are used.
			if (currentCpuSample != null){
				usePreviousReading=true;
				return previousCpuSample;
			}
		return currentCpuSample;
	}

	/**Returns the last cpu sample read from the cpu profile*/
	public String currentSample() {		
		return (!usePreviousReading) ? currentCpuSample : previousCpuSample;
	}

	public String previousSample() {		
		return (!usePreviousReading) ? previousCpuSample : antepenultimateCpuSample;
	}


	public String getProfileName() {		
		return profilename;
	}


	@Override
	public void closeReader() {
		try {
			cpuProfile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	/**this method modifies the time of the last cpusample with the newLastSampletime value passed as first argument.
	 *Returns a modified set of cpu samples with a modified last sample time
	 *NOTE: the modification process involves that samples with bigger time values than the newLastSampleTime are discarded until
	 *reach a sample with equal or less time value. If this sample is the first sample, the modification does not take effect.
	 **/
	public static String synchronizeLastCpuSampleTime(long newLastSampleTime, String cpusamples) {
			
		if(cpusamples!=null && cpusamples.compareTo("")!=0){			
			String[] samples = cpusamples.split("\n");
			
			int sampleIndex = samples.length-1;
			while (sampleIndex >= FIRST_SAMPLE){
				String currentLastSampleTime = CpuProfileReader.getSampleTime(samples[sampleIndex]);
				if (sampleIndex == FIRST_SAMPLE || Long.parseLong(currentLastSampleTime) <= newLastSampleTime){
					/*the logic nested in this branch is just to preserve variability in the cpu samples*/
					if (sampleIndex == samples.length-1)
						samples[sampleIndex]=samples[sampleIndex].replaceFirst(currentLastSampleTime, ((Long)newLastSampleTime).toString());
					else{
						currentLastSampleTime = CpuProfileReader.getSampleTime(samples[sampleIndex+1]);
						samples[sampleIndex+1]=samples[sampleIndex+1].replaceFirst(currentLastSampleTime, ((Long)newLastSampleTime).toString());
						sampleIndex+=1;
					}
					break;
				}
				else
					sampleIndex--;
			}
			String ret = "";
			int i = 0;
			while (i <= sampleIndex){//join valid cpusamples into a string, i.e., cpusamples starting from the newSampleTime required. 
				ret+=samples[i]+"\n";
				i++;
			}
			return ret;
		}
		else
			return cpusamples;
	}


	/**This method synchronizes the first sample time of the cpusamples receive as argument with the time received
	 * as first argument, avoiding the break of the timeline*/
	public static String synchronizeFirstCpuSampleTime(long firstSampleTime, String cpusamples) {
		if(cpusamples!=null && cpusamples.compareTo("")!=0){
			String[] samples = cpusamples.split("\n");			
			int sampleIndex = 0;
			while (sampleIndex < samples.length){
				String currentSampleTime = CpuProfileReader.getSampleTime(samples[sampleIndex]);
				if (sampleIndex == samples.length-1 || Long.parseLong(currentSampleTime) >= firstSampleTime){
					samples[sampleIndex]=samples[sampleIndex].replaceFirst(currentSampleTime, ((Long)firstSampleTime).toString());
					break;
				}
				else
					sampleIndex++;
			}
			String ret = "";
			while (sampleIndex < samples.length){//join valid cpusamples into a string, i.e., cpusamples starting from the newSampleTime required. 
				ret+=samples[sampleIndex]+"\n";
				sampleIndex++;
			}
			return ret;			
		}
		else
			return cpusamples;
	}


	public static boolean containsMoreThanOneSample(String cpusamples) {
		if (cpusamples != null){
			if (cpusamples.compareTo("")!=0){
				int firstOccurrence = cpusamples.indexOf("NEW_CPU_STATE_NODE");
				int lastOccurrence = cpusamples.lastIndexOf("NEW_CPU_STATE_NODE");
				return firstOccurrence!=lastOccurrence;
			}
		}		
		return false;
	}


	public static String extractFirstCpuSample(String cpusamples) {				
			if (cpusamples!= null && cpusamples.compareTo("")!=0) return cpusamples.substring(0,cpusamples.indexOf("\n"));
			
			return null;
	}	
}
