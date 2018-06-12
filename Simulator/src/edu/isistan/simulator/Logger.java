package edu.isistan.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileWriter;

/**
 * A utility class for logging messages into the console.
 */
public class Logger {
	
	private static OutputStream DEBUG_OUTPUT_STREAM = null; 
	
	/** The Constant LINE_SEPARATOR. */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/**
	 * Set data.separator to choose another way of separating the info in logs
	 */
	private static final String DATA_SEPARATOR = System.getProperty("data.separator")!=null ? System.getProperty("data.separator") : ";";

	/** The output. */
	private static OutputStream OUTPUT;	

	/** The disable output flag. */
	private static boolean ENABLE=true;
	
	public static final String NEW_LINE = "\r\n";
	
	public static  String EXPERIMENT = "";
	
	public static int FINISHED_JOB_INDEX = 0;
	
	public static void enable(){
		ENABLE=true;
	}
	
	public static void disable(){
		ENABLE=false;
	}
	
	public static void setOutput(OutputStream out){
		OUTPUT=out;
	}
	
	private static OutputStream getOutputStream() {
		if (OUTPUT == null)
			return System.out;
		return OUTPUT;
	}
	
	public static void print(String data){
		if (ENABLE)
			try {
				getOutputStream().write(data.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public static void println(String data){
		if (ENABLE)
			try {
				getOutputStream().write(data.getBytes());
				getOutputStream().write(LINE_SEPARATOR.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public static void println(){
		if (ENABLE)
			try {
				getOutputStream().write(LINE_SEPARATOR.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public static void println(Object data){
		if (ENABLE)
			println(data.toString());
	}
	
	public static void print(Object data){
		if (ENABLE)
			print(data.toString());
	}
	
	public static void logEntity(Entity e, String log, Object... data) {
		if (!ENABLE) return;
		StringBuffer logAux=new StringBuffer();
		logAux.append(Simulation.getTime());
		logAux.append(DATA_SEPARATOR);
		logAux.append(e.getName());
		logAux.append(DATA_SEPARATOR);
		logAux.append(log);
		for(Object o: data){
			logAux.append(DATA_SEPARATOR);
			logAux.append(o);			
		}
		println(logAux.toString());
	}
	
	/*Test Yisel*/
	public static void logGAIndividual(Short[] individual, String p)
	{
		if(true)
			return;
		String s = "";
		for (int i = 0; i < individual.length; i++) 
			s+=individual[i]+",";
		
		File file;
		boolean exists;
		FileWriter writer;
		file = new File("Yisel"+ p);
		exists = file.exists();
		try {
			writer = new FileWriter(file, true);
			writer.write(s + NEW_LINE);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*Test Yisel*/
	public static void logEntity2(Entity e, String log, Object... data) {
		if(true) return;
		StringBuffer logAux=new StringBuffer();
		logAux.append(Simulation.getTime());
		logAux.append(DATA_SEPARATOR);
		logAux.append(e.getName());
		logAux.append(DATA_SEPARATOR);
		logAux.append(log);
		for(Object o:data){
			logAux.append(DATA_SEPARATOR);
			logAux.append(o);			
		}
		println(logAux.toString());
	}
	
	/*Test Yisel*/
	public static void logEnergy(String log, Object... data) {
		if(true) return;
		StringBuffer logAux=new StringBuffer();
		logAux.append(Simulation.getTime());
		logAux.append(DATA_SEPARATOR);
		logAux.append(log);
		for(Object o:data){
			logAux.append(DATA_SEPARATOR);
			logAux.append(o);			
		}
		println(logAux.toString());
	}
	/*Test Yisel*/
	public static void logEnergy2(String log, Object... data) {
		if(true) return;
		StringBuffer logAux=new StringBuffer();
		logAux.append(Simulation.getTime());
		logAux.append(DATA_SEPARATOR);
		logAux.append(log);
		for(Object o:data){
			logAux.append(DATA_SEPARATOR);
			logAux.append(o);			
		}
		println(logAux.toString());
	}
	/*Test Yisel logJob*/
	public static void logJob(int jobId, String device, int batteryLevel, int jobInputSize, int jobOutputSize)
	{
		if(true) return;
		String fileName = "YiselJobs.csv";
		FINISHED_JOB_INDEX++;
		
		StringBuffer logAux = new StringBuffer();
		StringBuffer header = new StringBuffer();
		
		header.append("JobId,");
		logAux.append(jobId);
		logAux.append(",");
		
		header.append("JobIndex,");
		logAux.append(FINISHED_JOB_INDEX);
		logAux.append(",");
		
		header.append("Device,");
		logAux.append(device);
		logAux.append(",");
		
		header.append("BatteryLevel,");
		logAux.append(batteryLevel);
		logAux.append(",");
		
		header.append("Experiment,");
		logAux.append(EXPERIMENT);
		logAux.append(",");
		
		header.append("JobInputSize,");
		logAux.append(jobInputSize);
		logAux.append(",");
		
		header.append("JobOutputSize");
		logAux.append(jobOutputSize);
	
		WriteLog(fileName, header.toString(), logAux.toString());	
		
	}
	
	/*Test Yisel logJob*/
	public static void logJobDetails(int jobId, boolean rejected, boolean success, boolean successTrasferBack,
			long startTime, long startExecutionTime, long finishTime, long queueTime, long totalResultsTransferTime,
			long totalTransferTime) 
	{
		if(true) return;
		String fileName = "YiselJobsDetails.csv";
		
		StringBuffer logAux = new StringBuffer();
		StringBuffer header = new StringBuffer();
		
		header.append("JobId,");
		logAux.append(jobId);
		logAux.append(",");
		
		header.append("Experiment,");
		logAux.append(EXPERIMENT);
		logAux.append(",");
		
		header.append("Rejected,");
		logAux.append(rejected);
		logAux.append(",");
		
		header.append("Success,");
		logAux.append(success);
		logAux.append(",");
		
		header.append("SuccessTrasferBack,");
		logAux.append(successTrasferBack);
		logAux.append(",");
		
		header.append("StartTime,");
		logAux.append(startTime);
		logAux.append(",");
		
		header.append("StartExecutionTime,");
		logAux.append(startExecutionTime);
		logAux.append(",");
		
		header.append("FinishTime,");
		logAux.append(finishTime);
		logAux.append(",");
		
		header.append("QueueTime,");
		logAux.append(queueTime);
		logAux.append(",");
		
		header.append("TotalResultsTransferTime,");
		logAux.append(totalResultsTransferTime);
		logAux.append(",");
		
		header.append("TotalTransferTime");
		logAux.append(totalTransferTime);	
	
		WriteLog(fileName, header.toString(), logAux.toString());	
		
	}
	
	/*Test Yisel logDevice*/
	public static void logDevice(String device, int jobsScheduled, int jobsFinished, int pendingTransfers, int totalTransfers, short wifiRSSI, double energyPercentageWastedInNetworkActivity, double initialJoules, double accEnergyInTransfering){
		if(true) return;
		String fileName = "YiselDevices.csv";
		
		StringBuffer logAux = new StringBuffer();
		StringBuffer header = new StringBuffer();
		
		header.append("Device,");
		logAux.append(device);
		logAux.append(",");
		
		header.append("JobsScheduled,");
		logAux.append(jobsScheduled);
		logAux.append(",");
		
		header.append("JobsFinished,");
		logAux.append(jobsFinished);
		logAux.append(",");
		
		header.append("PendingTranfs,");
		logAux.append(pendingTransfers);
		logAux.append(",");
		
		header.append("TotalTranfs,");
		logAux.append(totalTransfers);
		logAux.append(",");
		
		header.append("WifiRSSI,");
		logAux.append(wifiRSSI);
		logAux.append(",");
		
		header.append("EnergyPercentageWastedInNetworkActivity,");
		logAux.append(energyPercentageWastedInNetworkActivity);
		logAux.append(",");
		
		header.append("InitialJoules,");
		logAux.append(initialJoules);
		logAux.append(",");
		
		header.append("AccEnergyInTransfering,");
		logAux.append(accEnergyInTransfering);
		logAux.append(",");
		
		header.append("Experiment,");
		logAux.append(EXPERIMENT);
	
		WriteLog(fileName, header.toString(), logAux.toString());	
	}
	
	/*Test Yisel logExperiment*/
	public static void logExperiment(int jobsArrived,int jobsScheduled, int jobsFinished, int jobsCompleted, double sentDataGB, double receivedDataGB, double percentEnergySendingData, double percentEnergyReceivingData, double totalGips, double executedGips){
		if(true) return;
		String fileName = "YiselExperiments.csv";
		
		StringBuffer logAux = new StringBuffer();
		StringBuffer header = new StringBuffer();
		
		header.append("Experiment,");
		logAux.append(EXPERIMENT);
		logAux.append(",");
		
		header.append("JobsArrived,");
		logAux.append(jobsArrived);
		logAux.append(",");
		
		header.append("JobsScheduled,");
		logAux.append(jobsScheduled);
		logAux.append(",");
		
		header.append("JobsFinished,");
		logAux.append(jobsFinished);
		logAux.append(",");
		
		header.append("JobsCompleted,");
		logAux.append(jobsCompleted);
		logAux.append(",");
		
		header.append("SentDataGB,");
		logAux.append(sentDataGB);
		logAux.append(",");
		
		header.append("ReceivedDataGB,");
		logAux.append(receivedDataGB);
		logAux.append(",");
		
		header.append("PercentEnergySendingData,");
		logAux.append(percentEnergySendingData);
		logAux.append(",");
		
		header.append("PercentEnergyReceivingData,");
		logAux.append(percentEnergyReceivingData);
		logAux.append(",");
		
		header.append("ExecutedGips,");
		logAux.append(executedGips);
		logAux.append(",");
		
		header.append("TotalGips,");
		logAux.append(totalGips);
		
		WriteLog(fileName, header.toString(), logAux.toString());	
	}
	
	/*Test Yisel logExperiment*/
	public static void logExperiment2(int arrived, int notScheduled, int inputTransferInterrupted, int notStarted, int startedButNotFinished, int outputTransferInterrupted, int completed, double sentDataGB, double receivedDataGB, double totalDataToTransferGB, double percentEnergySendingData, double percentEnergyReceivingData, double totalGips, double executedGips){
		if(true) return;
		String fileName = "YiselExperiments.csv";
		
		StringBuffer logAux = new StringBuffer();
		StringBuffer header = new StringBuffer();
		
		header.append("Experiment,");
		logAux.append(EXPERIMENT);
		logAux.append(",");
		
		header.append("JobsArrived,");
		logAux.append(arrived);
		logAux.append(",");
		
		header.append("JobsNotScheduled,");
		logAux.append(notScheduled);
		logAux.append(",");
		
		header.append("JobsInputTransferInterrupted,");
		logAux.append(inputTransferInterrupted);
		logAux.append(",");
		
		header.append("JobsNotStarted,");
		logAux.append(notStarted);
		logAux.append(",");
		
		header.append("JobsStartedButNotFinished,");
		logAux.append(startedButNotFinished);
		logAux.append(",");
		
		header.append("JobsOutputTransferInterrupted,");
		logAux.append(outputTransferInterrupted);
		logAux.append(",");
		
		header.append("JobsCompleted,");
		logAux.append(completed);
		logAux.append(",");
		
		header.append("SentDataGB,");
		logAux.append(sentDataGB);
		logAux.append(",");
		
		header.append("ReceivedDataGB,");
		logAux.append(receivedDataGB);
		logAux.append(",");
		
		header.append("TotalDataToTransferGB,");
		logAux.append(totalDataToTransferGB);
		logAux.append(",");
		
		header.append("PercentEnergySendingData,");
		logAux.append(percentEnergySendingData);
		logAux.append(",");
		
		header.append("PercentEnergyReceivingData,");
		logAux.append(percentEnergyReceivingData);
		logAux.append(",");
		
		header.append("ExecutedGips,");
		logAux.append(executedGips);
		logAux.append(",");
		
		header.append("TotalGips,");
		logAux.append(totalGips);
		
		WriteLog(fileName, header.toString(), logAux.toString());	
	}
	
	/*Test Yisel logEvent*/
	public static void logEvent(int srcId, long time, int trgId, int eventType){
		if(true) return;
		String header = "srcId, time, trgId, eventType";
		String fileName = "YiselEvents.csv";
		
		StringBuffer logAux = new StringBuffer();
		logAux.append(srcId);
		logAux.append(",");
		logAux.append(time);
		logAux.append(",");
		logAux.append(trgId);
		logAux.append(",");
		logAux.append(eventType);
		
		WriteLog(fileName, header, logAux.toString());	
	}
	
	
	private static void WriteLog(String fileName, String header,String content)
	{
		File file;
		boolean exists;
		FileWriter writer;
		file = new File(fileName);
		exists = file.exists();
		try {
			writer = new FileWriter(file, true);
			if (!exists && header != null && !header.trim().isEmpty()) {//then write the header
				writer.write(header + NEW_LINE);
			}
			writer.write(content + NEW_LINE);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	public static void appendDebugInfo(String line){
				
		try {
			DEBUG_OUTPUT_STREAM.write(line.getBytes());
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}

	public static void flushDebugInfo(){
		try {
			DEBUG_OUTPUT_STREAM.flush();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	public static void setDebugOutputStream(OutputStream debugFile) {
		DEBUG_OUTPUT_STREAM = debugFile;		
	}

}
