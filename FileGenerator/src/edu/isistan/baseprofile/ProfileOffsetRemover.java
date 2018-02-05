package edu.isistan.baseprofile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProfileOffsetRemover {

	public static void main(String[] args) {
		//ProfileOffsetRemover pfr= new ProfileOffsetRemover(args[0], args[1]);
		convertMillisIntoHourMinuteSecond(127388021);
	}

	public ProfileOffsetRemover(String profileIn, String profileOut) {
		ProfileReader profIn = null;
		if (profileIn.contains("bat")){
			profIn = new BatteryProfileReader(profileIn);
		}
		else
			if (profileIn.contains("cpu")){
				profIn = new CpuProfileReader(profileIn);
			}
		BufferedWriter profOut;
		try {
			profOut = new BufferedWriter(new FileWriter(new File(profileOut)));
			String currSample = profIn.currentSample();
			long offset = 1;
			long timeline = 1 + offset;//this is to force to the cpu sample start at the 2nd millisecond
			Long time = profIn.getCurrentSampleTime();
			if (profileIn.contains("bat")){
				profOut.write(profIn.currentSample() + "\n");				
				timeline = profIn.getCurrentSampleTime();
				profIn.readSample();//read first NEW_BATTERY_STATE sample
				time = profIn.getCurrentSampleTime();
				currSample = profIn.currentSample();
				timeline += offset;
			}
			currSample = currSample.replaceFirst(time.toString(),(new Long(timeline)).toString());
			profOut.write(currSample + "\n");
			profIn.readSample();
			
			while (profIn.currentSample() != null) {				
				offset = profIn.getCurrentSampleTime() - profIn.getPreviousSampleTime();
				if (offset > 0){
					timeline+=offset;
					time = profIn.getCurrentSampleTime();
					currSample = profIn.currentSample();
					currSample = currSample.replaceFirst(time.toString(),(new Long(timeline)).toString());
					profOut.write(currSample + "\n");
					profIn.readSample();
				}
				else
					System.out.println("Error: INCORRECT TIMELINE STEP");
			}
			profOut.flush();
			profOut.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	public static void convertMillisIntoHourMinuteSecond(long millis){		
		long hour = (long) Math.floor((millis / (1000 * 60 * 60)));		
		long minute = (long)Math.floor((millis / (1000 * 60))) - (hour * 60);		
		long second = (long) Math.floor((millis / 1000)) - ((hour * 60 * 60) + (minute * 60));
		long remainingMillis= millis - ((second * 1000) + (minute * 60 * 1000) + (hour * 60 * 60 * 1000));
		System.out.println(hour+":"+minute+":"+second+"."+remainingMillis);
	}

}
