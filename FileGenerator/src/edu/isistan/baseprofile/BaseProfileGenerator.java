package edu.isistan.baseprofile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import edu.isistan.baseprofile.usersession.length.ExpPlusPareto;
import edu.isistan.baseprofile.usersession.length.Exponential;
import edu.isistan.baseprofile.usersession.length.SessionLengthModel;
import edu.isistan.baseprofile.usersession.time.WeibullSessionTime;

public class BaseProfileGenerator {
	
	private static final int IDLE_PROFILE = 0;
	private ArrayList<BatteryProfileReader> batteryProfiles;
	private ArrayList<CpuProfileReader> 	cpuProfiles;
	private BufferedWriter batteryMixedProfile;
	private BufferedWriter cpuMixedProfile; 
	private long elapsedTime;
	
	private final int INVALID_ACC_USAGE=-1;
	private final String GENERIC_BATTERY_PROFILE_NAME="battery";
	private final String GENERIC_CPU_PROFILE_NAME="cpu";
	
	/**profiles should be provided from the largest to the shortest one, i.e., battery0 is supposed to be larger than
	 * battery30 because it contains more samples than the last, so, battery0 should appear first than battery30. 
	 * Profiles are indicated in a single string as first argument to the main and the separator character is ';'.
	 * To each battery profile the corresponding cpu profile should exists in the same directory. The cpu profile name 
	 * assumed by this program is the same as the name of the battery profile by replacing the string "baterry" by the
	 * string "cpu". Ej. when the file "baterry30.cnf" is provided as input, then the file "cpu30.cnf" should exist in the 
	 * same directory of baterry30.cnf*/
	public static void main(String[] args) {
		//testSessionLenghtModel();
		//testTimeBtwSessionModel();
		BaseProfileGenerator bpg = new BaseProfileGenerator();
		if (args.length > 0){
			String[] profileUsageFiles=args[0].split(";");
			String fileout="";
			if (args.length > 1)
				fileout=args[1];
			bpg.setProfilePaths(profileUsageFiles);
			//String directory=profileUsageFiles[0].substring(0, profileUsageFiles[0].lastIndexOf("/")+1);			
			bpg.generateBaseProfile(fileout);
		}
		else{
			System.out.println("Not enough parameters provided");
		}
	}
	

	public BaseProfileGenerator(){
		elapsedTime = 0;		
		this.batteryProfiles = new ArrayList<BatteryProfileReader>();
		this.cpuProfiles = new ArrayList<CpuProfileReader>();
	}
	

	private static void testTimeBtwSessionModel() {
		WeibullSessionTime timeBtwSessionDistribution=null;
		for (int i=0; i<10; i++){
			timeBtwSessionDistribution = new WeibullSessionTime();
			System.out.println(timeBtwSessionDistribution.toString());
		}
		System.out.println();
		System.out.println("sample, value");
		for (int i=0; i<1000; i++){
			System.out.println(i+","+timeBtwSessionDistribution.getTimeBtwSessionSample());
		}
	}


	private static void testSessionLenghtModel() {
		Exponential timeintervalDistribution=null;
		for (int i=0; i<1; i++){
			timeintervalDistribution = new Exponential();
			System.out.println(timeintervalDistribution.toString());
		}
		System.out.println();
		System.out.println("sample, value");
		for (int i=0; i<1000; i++){
			System.out.println(i+","+timeintervalDistribution.getSessionLenghtSample());
		}
	}


	private void setProfilePaths(String[] profileUsageFiles){
		System.out.print("battery_input_files");
		for (int i = 0; i < profileUsageFiles.length; i++){
			this.batteryProfiles.add(new BatteryProfileReader(profileUsageFiles[i]));
			System.out.print(";"+profileUsageFiles[i]);	
			int lastSlashIndex = profileUsageFiles[i].lastIndexOf("/");				
			String cpufile=(profileUsageFiles[i].substring(lastSlashIndex, profileUsageFiles[i].length())).replaceAll(GENERIC_BATTERY_PROFILE_NAME, GENERIC_CPU_PROFILE_NAME);
			this.cpuProfiles.add(new CpuProfileReader(profileUsageFiles[i].substring(0, lastSlashIndex)+cpufile));
		}
		System.out.println();
	}
	
	private void generateBaseProfile(String outprefix) {
		
		System.out.println("output_files;"+outprefix+"_battMixed.cnf;"+outprefix+"_cpuMixed.cnf");
		WeibullSessionTime timestampDistribution = new WeibullSessionTime();		
		ExpPlusPareto timeintervalDistribution = new ExpPlusPareto(2d,120d); 
		System.out.println("time_interval_function;"+timeintervalDistribution.toString());
		System.out.println("time_between_session_function;"+timestampDistribution.toString());		
		try {			
			batteryMixedProfile = new BufferedWriter(new FileWriter(new File(outprefix+"_battMixed.cnf")));		
			cpuMixedProfile = new BufferedWriter(new FileWriter(new File(outprefix+"_cpuMixed.cnf")));
		} catch (IOException e1) {			
			e1.printStackTrace();
		}
		ArrayList<BatteryProfileReader> removedBattProfiles = new ArrayList<BatteryProfileReader>();
		int profile=0;
		try {
			cpuMixedProfile.write("");
			batteryMixedProfile.write(batteryProfiles.get(profile).currentSample()+"\n");
			elapsedTime=batteryProfiles.get(profile).getCurrentSampleTime()+1;		
			batteryProfiles.get(profile).readSample();
			int percentage=batteryProfiles.get(profile).getCurrentSampleBatteryPercentage();	
			long timeIntervalUsage=getTimeBtwSessions(timestampDistribution);
			long previousInitTime = elapsedTime;
			long currentInitTime = previousInitTime;
			BatteryProfileReader previous_profile = null;
			int previous_usage_profile_index = 0;
			
			while(batteryProfiles.size()>0){				
				
				SampleSet usageSamples = batteryProfiles.size()>1 ? getUsageSamples(batteryProfiles.get(profile), cpuProfiles.get(profile),percentage, timeIntervalUsage): getUsageSamples(batteryProfiles.get(profile), cpuProfiles.get(profile),percentage, Long.MAX_VALUE);				
								
				
				if(usageSamples.getBatterySamples()!=null){
					if (usageSamples.getBatterySamples() != ""){
						percentage=batteryProfiles.get(profile).getCurrentSampleBatteryPercentage();
						currentInitTime = Long.parseLong(BatteryProfileReader.getSampleTime(BatteryProfileReader.extractFirstBatterySample(usageSamples.getBatterySamples())));					
						if (previous_profile != null)
							previous_profile.setAccumulatedUsage(previous_profile.getAccumulatedUsage()+(currentInitTime-previousInitTime));
						previousInitTime = currentInitTime;
						previous_profile = batteryProfiles.get(profile);
					}
					//assign a value to timeIntervalUsage based on the next usage profile. If the current is 0% the next will be x% so timeIntervalUsage is obtained with the timeIntervalDistribution
					timeIntervalUsage = profile==IDLE_PROFILE ? getTimeInterval(timeintervalDistribution): getTimeBtwSessions(timestampDistribution);
					batteryMixedProfile.write(usageSamples.getBatterySamples());					
					cpuMixedProfile.write(usageSamples.getCpuSamples());					
					if (BatteryProfileReader.hasLeftNodeSample(usageSamples.getBatterySamples())){//end of the mixed profile
						currentInitTime=Long.parseLong(BatteryProfileReader.getSampleTime(BatteryProfileReader.getLeftNodeSample(usageSamples.getBatterySamples())));
						previous_profile.setAccumulatedUsage(previous_profile.getAccumulatedUsage()+(currentInitTime-previousInitTime));
						closeBatteryProfiles(batteryProfiles);
						closeCpuProfiles(cpuProfiles);
						removedBattProfiles.addAll(batteryProfiles);
						batteryProfiles.clear();
					}
				}
				else{
					if(profile!=IDLE_PROFILE){//means that the mixer has reached the end of one intermediate input profile from which it extracts usage samples.   
						BatteryProfileReader pf = batteryProfiles.get(profile);
						/*since this is not a normal termination of the mixing process, it will be noted by setting
						  the accumulatedUsage in -1 value (as warning sign).*/
						pf.setAccumulatedUsage(INVALID_ACC_USAGE);
						pf.closeReader();
						removedBattProfiles.add(pf);
						batteryProfiles.remove(profile);						
						cpuProfiles.get(profile).closeReader();
						cpuProfiles.remove(profile);
					}
					else{//means that the mixer has reached the end of the first input profile which is assumed to be the largest one. So if the largest one has no more samples, make no sense to continue with the rest of the input profiles
						/*however, this is not the normal termination of the mixing process reason for what 
						  the accumulatedUsage  is set in -1 value (as warning sign)*/
						setInvalidAccumulatedUsage(batteryProfiles);
						closeBatteryProfiles(batteryProfiles);
						closeCpuProfiles(cpuProfiles);
						removedBattProfiles.addAll(batteryProfiles);						
						batteryProfiles.clear();						
					}
				}				
				previous_usage_profile_index= profile ==IDLE_PROFILE ? previous_usage_profile_index : profile;
				if (batteryProfiles.size() > 0 && profile==IDLE_PROFILE){
					if ((previous_usage_profile_index + 1) % batteryProfiles.size() == IDLE_PROFILE)//means that I have to use the first usage profile but not the first in the array (because the first in the array is the idle profile)
						profile= (previous_usage_profile_index + 2) % batteryProfiles.size();
					else
						profile= (previous_usage_profile_index + 1) % batteryProfiles.size();
				}
				else
					profile = IDLE_PROFILE;
			}	
			System.out.println("-------------------");
			System.out.println("Usage_profile_stats");
			double totalTime=0;
			for (java.util.Iterator<BatteryProfileReader> i = removedBattProfiles.iterator(); i.hasNext();){
				BatteryProfileReader bpr=i.next();
				totalTime+=bpr.getAccumulatedUsage();
				System.out.println("Acc. usage (in millis) of "+bpr.getProfileName()+":"+bpr.getAccumulatedUsage());
			}
			for (java.util.Iterator<BatteryProfileReader> i = removedBattProfiles.iterator(); i.hasNext();){
				BatteryProfileReader bpr=i.next();				
				System.out.println("Acc. usage (in percentage) of "+bpr.getProfileName()+":"+bpr.getAccumulatedUsage()*100/totalTime+"%");
			}
			System.out.println("-------------------");
			batteryMixedProfile.flush();		
			batteryMixedProfile.close();
			cpuMixedProfile.flush();
			cpuMixedProfile.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}

	private void setInvalidAccumulatedUsage(ArrayList<BatteryProfileReader> profileReaders) {
		for (BatteryProfileReader profileReader : profileReaders) {
			if (profileReader.getAccumulatedUsage()!=0){//means that this profile participate in the profile mixing at least once.
				profileReader.setAccumulatedUsage(INVALID_ACC_USAGE);
			}
		}
		
	}


	private void closeBatteryProfiles(ArrayList<BatteryProfileReader> profileReaders) {
		for (ProfileReader profileReader : profileReaders) {
			profileReader.closeReader();
		} 
		
	}
	
	private void closeCpuProfiles(ArrayList<CpuProfileReader> profileReaders) {
		for (ProfileReader profileReader : profileReaders) {
			profileReader.closeReader();
		} 
		
	}

	
	private SampleSet getUsageSamples(BatteryProfileReader batteryProfileReader, CpuProfileReader cpuProfileReader,
			int batteryPercentage, long timeIntervalUsage) {
		String battsamples="";	
		String cpusamples="";
		System.out.print("profile_"+batteryProfileReader.getProfileName()+";");
		if (timeIntervalUsage > 0){
			//A synchronization operation is performed looking for the first sample with the batteryPercentage passed as argument		
			batteryProfileReader.readUntilSampleWithPercentage(batteryPercentage);
			String notSynchronizedFirstBattSample = batteryProfileReader.currentSample();
			battsamples = getSynchronizedSamplesWithinRange(batteryProfileReader,timeIntervalUsage, elapsedTime);
			
			if (battsamples != null && battsamples.compareTo("")!=0){				
				long initSamplesTime = Long.parseLong(BatteryProfileReader.getSampleTime(BatteryProfileReader.extractFirstBatterySample(battsamples)));
				System.out.print(initSamplesTime);
				String sampleTime = BatteryProfileReader.getSampleTime(BatteryProfileReader.extractLastBatterySample(battsamples));
				long lastSampleTime = Long.parseLong(sampleTime); 
				//batteryProfileReader.setAccumulatedUsage(batteryProfileReader.getAccumulatedUsage()+(lastSampleTime-initSamplesTime));
				
				cpuProfileReader.readUntilSampleWithTime(Long.parseLong(BatteryProfileReader.getSampleTime(notSynchronizedFirstBattSample)));
				if (cpuProfileReader.currentSample() != null){
					cpusamples = getSynchronizedSamplesWithinRange(cpuProfileReader, lastSampleTime-initSamplesTime, initSamplesTime);
					
					/*Since the first cpu sample acquired in the previous sentence may not start exactly at the same time battery
					 * samples start, a synchronization operation of both init time is perform*/
					cpusamples = CpuProfileReader.synchronizeFirstCpuSampleTime(initSamplesTime,cpusamples);				 
					
					/*now, avoiding that last cpu sample overlaps with the next profile usage, a synchronization is performed with
					the last battery sample*/ 
					if (initSamplesTime < lastSampleTime && CpuProfileReader.containsMoreThanOneSample(cpusamples))						
						cpusamples = CpuProfileReader.synchronizeLastCpuSampleTime(lastSampleTime,cpusamples);				
				}
				elapsedTime=lastSampleTime+1;
			}
			
		}
		System.out.println();
		return new SampleSet(battsamples, cpusamples);
	}
	
	/**Using the ProfileReader received as argument, this method returns the list of samples within the time interval
	 * received as argument. The time of all returned samples is synchronized with the offsetTime received as argument
	 */
	private String getSynchronizedSamplesWithinRange(ProfileReader profileReader, long timeInterval, long offtime) {
		
		String sample = profileReader.currentSample();
		String sampleList= sample != null ? "" : null;
		
		long timeDiffBtwSamples=profileReader.getCurrentSampleTime()-profileReader.getPreviousSampleTime();//this variable is to preserve the step between samples
		/*if(timeDiffBtwSamples < 0){
			System.out.println("WARN: current sample time less than previous sample time");
		}*/
		
		long targetSampleTime = profileReader.getCurrentSampleTime() + timeInterval;
		long localElapsedTime = offtime;
				
		while(sample!=null && profileReader.getCurrentSampleTime() <= targetSampleTime){			
			sample = synchronizeSample(sample,localElapsedTime+timeDiffBtwSamples);
			localElapsedTime+=timeDiffBtwSamples;
			sampleList+=sample+"\n";
			profileReader.readSample();
			sample = profileReader.currentSample();							
			//now update the difference in milliseconds between the last and the current sample
			timeDiffBtwSamples=profileReader.getCurrentSampleTime()-profileReader.getPreviousSampleTime();
		}	
		
		return sampleList;
	}

	/**This method increments the elapsed time with dif value and returns the sample with a synchronized value of its timestamp*/
	private String synchronizeSample(String usageSample, long elapsedtime) {
		 	
			StringTokenizer st=new StringTokenizer(usageSample, ";");
			st.nextToken();
			Long currentUsageSampleTime = Long.parseLong(st.nextToken());			
			return usageSample.replaceFirst(currentUsageSampleTime.toString(),((Long)(elapsedtime)).toString());		
	}

	private long getTimeInterval(SessionLengthModel timeintervalDistribution) {
		return toMilliseconds(Math.round(timeintervalDistribution.getSessionLenghtSample()));		
	}


	private long getTimeBtwSessions(WeibullSessionTime timestampDistribution) {	
		double sampleValue=timestampDistribution.getTimeBtwSessionSample();
		return toMilliseconds(Math.round(sampleValue));		
	}


	private long toMilliseconds(long timeInSeconds) {		
		return timeInSeconds*1000 > Long.MAX_VALUE ? Long.MAX_VALUE : timeInSeconds*1000;
	}
	
	

}
