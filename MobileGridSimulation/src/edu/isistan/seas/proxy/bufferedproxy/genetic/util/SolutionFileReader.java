package edu.isistan.seas.proxy.bufferedproxy.genetic.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SolutionFileReader {
	
	private String file;
	private BufferedReader solutionContiner;	
	private String line;
	private int solutionSize;
	
	public SolutionFileReader(String file, int solutionSize){
		this.file = file;
		this.solutionSize = solutionSize;
	}
	
	public Short[] loadSolution() throws IOException{
		solutionContiner = new BufferedReader(new FileReader(file));
		
		this.nextLine();
		Short[] solution = new Short[solutionSize];
		int jobPosition = 0;
		while (line!=null){
			solution[jobPosition]=Short.parseShort(line);
			jobPosition++;
			this.nextLine();
		}
		
		return solution;
	}
	
	private void nextLine() throws IOException{
		this.line=this.solutionContiner.readLine();
		if(line==null) return;
		this.line=this.line.trim();
		while(line.startsWith("#")||
				line.equals(""))
			this.line=this.solutionContiner.readLine().trim();
	}

}
