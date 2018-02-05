package edu.isistan.nodetranslator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class NodeProfileTranslator {

	private long startTime;
	private List<Pair<Long, Integer>> battery=new ArrayList<Pair<Long,Integer>>();
	private List<Pair<Long, Double>> cpu=new ArrayList<Pair<Long,Double>>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NodeProfileTranslator t=new NodeProfileTranslator();
		t.read(args[0]);
		t.saveBattery(args[1]);
		if(args.length>=3) t.saveCPU(args[2]);
	}

	public void saveBattery(String string) {
		try {
			BufferedWriter bw=new BufferedWriter(new FileWriter(string));
			int last=-1;
			for(Pair<Long,Integer> p:this.battery)
				if(last!=p.getData().intValue()){
					bw.write(p.toString()+"\n");
					last=p.getData();
				}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}

	public void saveCPU(String string) {
		try {
			BufferedWriter bw=new BufferedWriter(new FileWriter(string));
			this.cpu.get(0).setTime(1L);
			for(Pair<Long,Double> p:this.cpu)
				bw.write(p.toString()+"\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void read(String string) {
		try {
			BufferedReader rd=new BufferedReader(new FileReader(string));
			String line=rd.readLine();
			if(line.contains("CPU"))
				this.startTime=Long.parseLong(line.substring(0, line.indexOf(",")));
			else{
				String l2=line.substring(line.indexOf(",")+1);
				this.startTime=Long.parseLong(l2.substring(0, l2.indexOf(",")));
			}
			while(line!=null){
				if(line.contains("CPU"))
					this.parceCPU(line);
				else if(!line.trim().equals(""))
					this.parceBattery(line);
				line=rd.readLine();
			}
		} catch (Exception e) {	
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void parceBattery(String line) {
		StringTokenizer st=new StringTokenizer(line,",");
		st.nextToken();
		long time=Long.parseLong(st.nextToken())-this.startTime;
		st.nextToken();
		int bat=Integer.parseInt(st.nextToken());
		this.battery.add(new Pair<Long, Integer>(time, bat));
	}

	private void parceCPU(String line) {
		long time=Long.parseLong(line.substring(0, line.indexOf(",")))-this.startTime;
		String d=line.substring(line.indexOf(" ")+1);
		double cpu=Double.parseDouble(d.substring(0, d.indexOf(" ")));
		this.cpu.add(new Pair<Long, Double>(time, cpu));
	}

}
