package edu.isistan.baseprofile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class BatteryProfileReader implements ProfileReader{
	
	private static final int LEFT_NODE_STRINGSIZE = 9;
	private long accumulatedUsage;
	private BufferedReader batteryProfile;
	private String currentBattSample;
	private String previousBattSample;	
	private String profilename;
	
	/**Given a battery sample, this method returns its time.
	 * The sample is bound to the format:
	 * event_name;time*/
	public static String getSampleTime(String sample){
		if (sample !=null && sample!=""){
			StringTokenizer st = new StringTokenizer(sample,";");
			st.nextToken();
			return st.nextToken();
		}
		else
			return "-1";
	}
	
	public BatteryProfileReader(String file){
		try {
			profilename=file.substring(file.lastIndexOf("/")+1, file.lastIndexOf(".cnf"));
			accumulatedUsage=0;			
			batteryProfile = new BufferedReader(new FileReader(new File(file)));
			this.readSample();			
			previousBattSample=null;			
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		
	}
	
	public String getProfileName(){
		return profilename;
	}
	
	public void setAccumulatedUsage(long accUsage){
		accumulatedUsage=accUsage;
	}
	
	public long getAccumulatedUsage(){
		return accumulatedUsage;
	}
	
	/**Return the next line of the profile. The method passes over the empty lines.*/
	public void readSample(){
		try {
			previousBattSample=currentBattSample;			
			currentBattSample=batteryProfile.readLine();			
		
			while (currentBattSample != null && currentBattSample.trim().equals("")){					
					currentBattSample=batteryProfile.readLine();
			}						
		} catch (IOException e) {
			e.printStackTrace();			
		}
	}
	
	public long getCurrentSampleTime(){
		if (currentBattSample!= null){
			StringTokenizer st=new StringTokenizer(currentBattSample, ";");
			st.nextToken();
			return Long.parseLong(st.nextToken());
		}
		else{
			//System.out.println("WARN: current battery sample time is -1 because current battery sample is null");
			return -1;
		}
	}
	
	public long getPreviousSampleTime(){
		if (previousBattSample== null) return 0;
		
		StringTokenizer st=new StringTokenizer(previousBattSample, ";");
		if(st.countTokens()==2){
			if (st.nextToken().compareTo("ADD_NODE")==0)
				return getCurrentSampleTime();
			else
				return Long.parseLong(st.nextToken());
		}				
		st.nextToken();
		return Long.parseLong(st.nextToken());
		
	}	

	public int getCurrentSampleBatteryPercentage() {		
		if (currentBattSample!= null){
			StringTokenizer st=new StringTokenizer(currentBattSample, ";");
			//Expected sample structure has four fields. Example: NEW_BATTERY_STATE_NODE;2;2276;9900000						
			if (st.countTokens() >= 4){
				st.nextToken();
				st.nextToken();
				st.nextToken();
			    return Integer.parseInt(st.nextToken());
			}
			else//if currentLine is the first (ADD_NODE) or the last (LEFT_NODE) of the profile, then the expected structure
				//has only two fields.				
			{
				if(st.nextToken().compareTo("ADD_NODE")==0){
					return 10000000;//the first value of battery profiles (should be fixed if nodes start with less battery level than this value)
				}
				else
					return 0000;
			}			
		}
		else{
			return -1;
		}		
	}
	
	

	/**Leaving from the currentSample, this method read samples of the profile until finding the first one that contains a value of percentage equal or greater than the batteryPercentage passed as argument
	 * The profile is assumed to be ordered by batteryPercentage.
	 * A null value could be returned if the batteryLevel is not found with a sequential search from the currentSample*/
	public String readUntilSampleWithPercentage(int batteryPercentage) {		
		while (currentBattSample != null && this.getCurrentSampleBatteryPercentage() > batteryPercentage)
			this.readSample();
		return currentBattSample;
	}

	
	public boolean batteryLeftNodeSample() {
		if (currentBattSample!= null){
			StringTokenizer st=new StringTokenizer(currentBattSample, ";");
			return st.nextToken().compareTo("LEFT_NODE")==0;
		}
		return false;
	}

	/**Returns the last battery sample read from the battery profile*/
	public String currentSample() {		
		return currentBattSample;
	}

	@Override
	public String previousSample() {		
		return previousBattSample;
	}

	@Override
	public void closeReader() {
		try {
			batteryProfile.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
	}

	public static String extractLastBatterySample(String battsamples) {
		if (battsamples != null && battsamples.compareTo("")!=0){
			int lastNewBatteryEvent=battsamples.lastIndexOf("NEW_BATTERY_STATE_NODE");
			if (lastNewBatteryEvent>-1 && !hasLeftNodeSample(battsamples)){
				return battsamples.substring(lastNewBatteryEvent, battsamples.length()-1);
			}
			else{
				if (lastNewBatteryEvent > -1){
					return battsamples.substring(lastNewBatteryEvent, battsamples.indexOf("LEFT_NODE")-1);
				}
				return battsamples.substring(battsamples.indexOf("LEFT_NODE"), battsamples.length()-1);
				
			}
		}
		return null;
	}
	
	public static String getLeftNodeSample(String battsamples){
		if (battsamples != null && battsamples.compareTo("")!=0 && hasLeftNodeSample(battsamples))
			return battsamples.substring(battsamples.indexOf("LEFT_NODE"),battsamples.length()-1);
		else
			return null;
	}

	public static boolean hasLeftNodeSample(String battsamples) {
		if (battsamples!= null && battsamples.compareTo("")!=0){
			int lastSemicolon=battsamples.lastIndexOf(";");
			return (lastSemicolon>=LEFT_NODE_STRINGSIZE && battsamples.substring(lastSemicolon-LEFT_NODE_STRINGSIZE, lastSemicolon).compareTo("LEFT_NODE")==0);//means that LEFT_NODE sample of the profile was reached
		}		
		return false;

	}

	public static String extractFirstBatterySample(String battsamples) {		
		if (battsamples!= null && battsamples.compareTo("")!=0) return battsamples.substring(0,battsamples.indexOf("\n"));
		
		return null;
		
	}

}
