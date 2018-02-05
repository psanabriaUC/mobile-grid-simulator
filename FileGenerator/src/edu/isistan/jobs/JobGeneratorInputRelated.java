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

public class JobGeneratorInputRelated {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length<9){
			System.err.println("nOfJobs millisMinTime millisMaxTime opsFunction:[nlogn|n_2|n_3|all] maxFlop bytesMinInput bytesMaxInput bytesMinOutput bytesMaxOutput");
		}		
		int nJobs=Integer.parseInt(args[0]);
		long minTime=Long.parseLong(args[1]); 
	    long maxTime=Long.parseLong(args[2]);
		String opsFunction=args[3].split(":")[1];//given the parameter format ParamName:Function, instantiate the opsFunction variable with Function value
		long maxFlop=Long.parseLong(args[4]);
		int minInput=Integer.parseInt(args[5]);
		int maxInput=Integer.parseInt(args[6]);
		int minOutput=Integer.parseInt(args[7]);
		int maxOutput=Integer.parseInt(args[8]);
		System.out.println("#input: numberOfJobs JobsArriveMinTime(millis) JobsArriveMaxTime(millis) opsFunction:[nlogn|n_2|n_3|all] maxFlop JobsMinInput(bytes) JobsMaxInput(bytes) JobsMinOutput(bytes) JobsMaxOutput(bytes)");
		System.out.println("#input: "+nJobs+" "+minTime+" "+maxTime+" "+opsFunction+" "+maxFlop+" "+minInput+" "+maxInput+" "+minOutput+" "+maxOutput);
		System.out.println("#output: id;ops;time;input;output");
		JobGeneratorInputRelated jg=new JobGeneratorInputRelated();
		InputStream is=jg.generateJobs(nJobs, minTime, maxTime, opsFunction, maxFlop, minInput, maxInput, minOutput, maxOutput);
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
	
	public InputStream generateJobs(int cant,long minTime,long maxTime, String opsFunction, long maxFlop,int minInput, int maxInput, int minOutput, int maxOutput){
		List<JobInformation> jobs=new ArrayList<JobInformation>(cant);
		for(int i=0;i<cant;i++){
			jobs.add(this.generateNewJob(i,opsFunction, maxFlop,minTime,maxTime,minInput,maxInput,minOutput,maxOutput));
		}
		Collections.sort(jobs);
		byte[] memory=this.transToMemory(jobs);
		return new ByteArrayInputStream(memory);
	}

	private JobInformation generateNewJob(int id, String opsFunction, long maxFlop,
			long minTime, long maxTime, int minInput, int maxInput,
			int minOutput, int maxOutput) {
		
		long time =( (long) ( Math.random()*(maxTime-minTime)) ) + minTime;
		int input =( (int) ( Math.random()*(maxInput-minInput)) ) + minInput;
		int output =( (int) ( Math.random()*(maxOutput-minOutput)) ) + minOutput;
		
		long inputEntries = (long)(input/1024);
		long ops = getOps(opsFunction, inputEntries);
		
		while (ops >= maxFlop) ops = getOps(opsFunction, inputEntries);
		
		return new JobInformation(time,id,ops,input,output);
	}

	private long getOps(String opsFunction, long inputEntries){
		
		if(opsFunction.compareTo("none")==0)
			return 0;
		if(opsFunction.compareTo("fixed")==0)
			return 83629400;
		if(opsFunction.compareTo("nlogn")==0)
			return (long) ((long)Math.log(inputEntries) * inputEntries );
				
		if (opsFunction.startsWith("n_")){
			String[] opsFunctionComponents = opsFunction.split("_");
			int exp = Integer.parseInt(opsFunctionComponents[1]);
			return (long) ((long)Math.pow(inputEntries, exp));
		}
		
		if (opsFunction.compareTo("all")==0){
			int function = ((int)(Math.random()*10000000))%3;
			switch (function){
			case 0:
				return (long) ((long)Math.log(inputEntries) * inputEntries );				
			case 1:
				return (long) ((long)Math.pow(inputEntries, 2));				
			case 2:
				return (long) ((long)Math.pow(inputEntries, 3));				
			}
		}
		return -1;
	}
	
	private byte[] transToMemory(List<JobInformation> jobs) {
		ByteArrayOutputStream bo=new ByteArrayOutputStream();
		BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(bo));
		try {
			while(jobs.size()>0){
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
