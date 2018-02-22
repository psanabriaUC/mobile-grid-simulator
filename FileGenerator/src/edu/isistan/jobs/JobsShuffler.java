package edu.isistan.jobs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**Given an already created jobs file, this class provides methods for change the time of jobs without altering the
 * rest of their features.
 * */
public class JobsShuffler {

	public static void main(String[] args) {
		
		ArrayList<JobInformation> jobs= new ArrayList<>();
		
		if(args.length < 1) {
			System.err.println("A jobs file must be provided as argument along its creation parameters related to jobs minTime and maxTime");
		}
		String jobsFile=args[0];
		long minTime = Long.parseLong(args[1]);
	    long maxTime = Long.parseLong(args[2]);
		try {
			BufferedReader br = new BufferedReader(new FileReader(jobsFile));
			String line = br.readLine();
			while (line != null){
				if (!line.startsWith("#")){
					String[] jobParts=line.split(";");
					long newtime =( (long) ( Math.random()*(maxTime-minTime)) ) + minTime;
					jobs.add(new JobInformation(newtime,Integer.parseInt(jobParts[0]),Long.parseLong(jobParts[1]),Integer.parseInt(jobParts[3]),Integer.parseInt(jobParts[4])));
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Collections.sort(jobs);
		for (JobInformation jobInformation : jobs) {
			System.out.println(jobInformation.toString());
		}
		;
	}

}
