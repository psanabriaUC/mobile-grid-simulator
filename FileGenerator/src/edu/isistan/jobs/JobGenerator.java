package edu.isistan.jobs;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobGenerator {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length<5){
			System.err.println("nOfJobs minTime maxTime minOps maxOps [minInput maxInput minOutput maxOutput]");
		}
		int nJobs=Integer.parseInt(args[0]);
		long minTime=Long.parseLong(args[1]); 
	    long maxTime=Long.parseLong(args[2]);
		long minOps=Long.parseLong(args[3]); 
		long maxOps=Long.parseLong(args[4]);
		int minInput=0;
		int maxInput=0;
		int minOutput=0;
		int maxOutput=0;
		if(args.length>5){
			minInput=Integer.parseInt(args[5]);
			maxInput=Integer.parseInt(args[6]);
			minOutput=Integer.parseInt(args[7]);
			maxOutput=Integer.parseInt(args[8]);
		}
		JobGenerator jg=new JobGenerator();
		InputStream is=jg.generateJobs(nJobs, minTime, maxTime, minOps, maxOps, minInput, maxInput, minOutput, maxOutput);
		OutputStream os=new ByteArrayOutputStream();
		byte[] bytes=new byte[1024];
		try{
			int readed=is.read(bytes);
			while(readed==1024){
				os.write(bytes);
				readed=is.read(bytes);
			}
			if(readed!=-1){
				os.write(bytes, 0, readed);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		System.out.println(new String(((ByteArrayOutputStream)os).toByteArray()));
	}
	
	public InputStream generateJobs(int cant,long minTime,long maxTime, long minOps, long maxOps,int minInput, int maxInput, int minOutput, int maxOutput){
		List<JobInformation> jobs=new ArrayList<JobInformation>(cant);
		for(int i=0;i<cant;i++){
			jobs.add(this.generateNewJob(i, minOps, maxOps,minTime,maxTime,minInput,maxInput,minOutput,maxOutput));
		}
		Collections.sort(jobs);
		byte[] memory=this.transToMemory(jobs);
		return new ByteArrayInputStream(memory);
	}

	private JobInformation generateNewJob(int id, long minOps, long maxOps,
			long minTime, long maxTime, int minInput, int maxInput,
			int minOutput, int maxOutput) {
		long ops = ( (long) ( Math.random()*(maxOps-minOps)) ) + minOps;
		long time =( (long) ( Math.random()*(maxTime-minTime)) ) + minTime;
		int input =( (int) ( Math.random()*(maxInput-minInput)) ) + minInput;
		int output =( (int) ( Math.random()*(maxOutput-minOutput)) ) + minOutput;
		return new JobInformation(time,id,ops,input,output);
	}

	private byte[] transToMemory(List<JobInformation> jobs) {
		ByteArrayOutputStream bo=new ByteArrayOutputStream();
		BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(bo));
		try {
			while(jobs.size() > 0){
				writer.write(jobs.remove(0).toString()+"\n");	
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bo.toByteArray();
	}

}
