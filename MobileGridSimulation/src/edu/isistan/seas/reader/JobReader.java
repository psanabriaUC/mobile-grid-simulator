package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

public class JobReader extends Thread {

	private ReentrantLock simLock;
	private ReentrantLock eventLock;
	private BufferedReader conf;
	private boolean networkMeasurementEnable = false;
	
	public JobReader(ReentrantLock simLock, ReentrantLock eventLock, BufferedReader conf, boolean networkEnableFlag) {
		super();
		this.simLock=simLock;
		this.eventLock=eventLock;
		this.conf=conf;
		this.networkMeasurementEnable = networkEnableFlag;
	}

	/**
	 * Jobs FileFormat
	 * ops;time;inputSize;outputSize*
	 */
	@Override
	public void run() {
		try{
			int id=1;
			this.simLock.lock();
			int src=Simulation.getEntityId("PROXY");
			this.simLock.unlock();
			String line=this.conf.readLine();
			boolean lackingJobParameter = false;
			while(line!=null){
				line=line.trim();
				if(line.equals("")||line.startsWith("#")){
					line=this.conf.readLine();
				} else {
					StringTokenizer ts=new StringTokenizer(line, ";");
					ts.nextToken();
					long ops=Long.parseLong(ts.nextToken());
					long time=Long.parseLong(ts.nextToken());
						
					int jobId=id;
					id++;

					int inputSize=0;
					int outputSize=0;
					if(ts.hasMoreTokens()){
						inputSize=Integer.parseInt(ts.nextToken());
						outputSize=Integer.parseInt(ts.nextToken());
					}
					else
						if (networkMeasurementEnable && (inputSize == 0 || outputSize == 0)){
							lackingJobParameter = true;
						}
					Job j=new Job(jobId, ops, src, inputSize, outputSize);

					this.eventLock.lock();
					Event e=Event.createEvent(Event.NO_SOURCE, time, src, SchedulerProxy.EVENT_JOB_ARRIVE, j);
					this.eventLock.unlock();

					this.simLock.lock();
					Simulation.addEvent(e);
					this.simLock.unlock();

					line=this.conf.readLine();
				}
			}
			if (lackingJobParameter){
				System.out.println("[WARN] At least one job has no input size or output size defined");
			}
		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	

}
