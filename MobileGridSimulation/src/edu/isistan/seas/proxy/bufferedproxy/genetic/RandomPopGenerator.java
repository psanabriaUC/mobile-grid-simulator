package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.Random;

import edu.isistan.mobileGrid.jobs.Job;

public class RandomPopGenerator extends PopulationGenerator {

	public RandomPopGenerator(){
		super();
	}
	
	@Override
	public ArrayList<Short[]> generatePopulation(ArrayList<Job> jobs, int totalIndividuals, int individualChromosomes, int chromoMaxValue) {
		
		ArrayList<Short[]> randPopulation = new ArrayList<Short[]>();
		Random rand = new Random();
		
		for (int individualIndex = 0; individualIndex < totalIndividuals; individualIndex++) {
			Short[] individual = new Short[individualChromosomes];
			for (int chromosome = 0; chromosome < individualChromosomes; chromosome++)				
				individual[chromosome] = (short) (rand.nextInt(chromoMaxValue + 1) - 1);
			randPopulation.add(individual);
		}

		return randPopulation;
	}

}
