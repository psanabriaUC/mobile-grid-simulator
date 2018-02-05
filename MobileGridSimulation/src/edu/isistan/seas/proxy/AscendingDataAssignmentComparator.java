package edu.isistan.seas.proxy;

import java.util.Comparator;

public class AscendingDataAssignmentComparator implements
		Comparator<DataAssignment> {

	DataAssignmentEvaluatorIF evaluator;
	
	public AscendingDataAssignmentComparator(DataAssignmentEvaluatorIF evaluator){
		this.evaluator = evaluator;
	}
	
	@Override
	public int compare(DataAssignment da1, DataAssignment da2) {
		double da1Value = da1.eval(); 
		double da2Value = evaluator.eval(da2);
		
		if (da1Value > da2Value){
			return 1;		
		}
		else{
			if (da1Value < da2Value){
				return -1;
			}
			else
				return 0;
		}
	}

}
