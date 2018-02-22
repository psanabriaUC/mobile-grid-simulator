package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

/**
 * Helper class for reading and parsing a list of {@link Job}s contained in a configuration file. Examples input files can be found in
 * sim_input/jobs/*.
 *
 * This class provides its own Runnable implementation so it can be run in parallel.
 */
public class JobReader extends Thread {

	private ReentrantLock simLock;
	private BufferedReader conf;
	private boolean networkMeasurementEnable = false;
	
	public JobReader(ReentrantLock simLock, BufferedReader conf, boolean networkEnableFlag) {
		super();
		this.simLock = simLock;
		this.conf = conf;
		this.networkMeasurementEnable = networkEnableFlag;
	}

	/**
	 * Jobs FileFormat
	 * ops;time;inputSize;outputSize*
	 */
	@Override
	public void run() {
		try{
			this.simLock.lock();
			int schedulerProxyId = Simulation.getEntity("PROXY").getId();
			this.simLock.unlock();

			String line = this.conf.readLine();
			boolean lackingJobParameter = false;

			// Expected format for each line in the configuration file:
			// [jobId];[# of CPU cycles required to complete];[arrival time](;[input size];[output size])?
			while (line != null){
				line = line.trim();
				if (line.equals("") || line.startsWith("#")) {
					line = this.conf.readLine();
				} else {
					StringTokenizer ts = new StringTokenizer(line, ";");
					// Currently the ID defined in the configuration file is ignored, instead it is automatically assigned by the engine.
					ts.nextToken();
					long ops = Long.parseLong(ts.nextToken());
					long time = Long.parseLong(ts.nextToken());

					int inputSize = 0;
					int outputSize = 0;
					if (ts.hasMoreTokens()) {
						inputSize = Integer.parseInt(ts.nextToken());
						outputSize = Integer.parseInt(ts.nextToken());
					} else if (networkMeasurementEnable) {
						lackingJobParameter = true;
                    }

                    // For each job, we create an encompassing event and send it to the scheduling proxy chosen for this simulation.
                    // The scheduling proxy will then re-send the events for processing to the appropriate devices according to its policy.
					Job job = new Job(ops, schedulerProxyId, inputSize, outputSize);
					Event event = Event.createEvent(Event.NO_SOURCE, time, schedulerProxyId, SchedulerProxy.EVENT_JOB_ARRIVE, job);

					this.simLock.lock();
					Simulation.addEvent(event);
					this.simLock.unlock();

					line = this.conf.readLine();
				}
			}
			if (lackingJobParameter) {
				System.out.println("[WARN] At least one job has no input size or output size defined");
			}
		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
