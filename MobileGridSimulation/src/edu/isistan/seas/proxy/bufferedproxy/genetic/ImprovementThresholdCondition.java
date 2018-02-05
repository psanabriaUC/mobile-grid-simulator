package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.Iterator;

public class ImprovementThresholdCondition implements TerminationCondition {

	private double minFitnessImprovementThreshold;
	private int minEvolutionCycles = 50;		
	private FitnessFunction fitnessFunction;
	private double previousGenerationMeanFitness = 0;
	private int currentCycle = 0;
	
	
	public ImprovementThresholdCondition(double threshold, int evolutionCycles,	FitnessFunction assignmentFitnessFunction) {
		this.minFitnessImprovementThreshold = threshold;
		this.minEvolutionCycles = evolutionCycles;
		this.fitnessFunction = assignmentFitnessFunction;
	}


	@Override
	public boolean satisfiedCondition(ArrayList<Short[]> population) {
		
		if (currentCycle == minEvolutionCycles){			
			/*currentCycle = 0;			
			double currentGenerationMeanFitness = 0;			
			for (Iterator<Short[]> iterator = population.iterator(); iterator.hasNext();) {
				Short[] individual =  iterator.next();
				double currentIndividualFitness = fitnessFunction.evaluate(individual);
				if (currentIndividualFitness > currentGenerationBestFitness){
					currentGenerationBestFitness = currentIndividualFitness; 
				}
				currentGenerationMeanFitness += fitnessFunction.evaluate(individual);
			}
			currentGenerationMeanFitness = currentGenerationMeanFitness/population.size();
			System.out.println("Mean fitness: "+currentGenerationMeanFitness);
			boolean ret = (currentGenerationMeanFitness - previousGenerationMeanFitness >= minFitnessImprovementThreshold) ? false : true;			
			previousGenerationMeanFitness = currentGenerationMeanFitness;
			return ret;*/			
			return true;
		}
		else{
			currentCycle++;
			return false;
		}
	}


	@Override
	public String getName() {		
		return this.getClass().getSimpleName()+" minFitnessImprovementThreshold="+this.minFitnessImprovementThreshold+" minEvolutionCycles="+this.minEvolutionCycles;
	}

}
