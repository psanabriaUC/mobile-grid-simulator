package edu.isistan.seas.proxy;

import java.util.Comparator;

public class DescendingDataAssignmentComparator implements
		Comparator<DataAssignment> {

	DataAssignmentEvaluatorIF evaluator;
	
	public DescendingDataAssignmentComparator(DataAssignmentEvaluatorIF eval){
		evaluator=eval;
	}
	
	
	@Override
	public int compare(DataAssignment da1, DataAssignment da2) {
		
		double da1Value = evaluator.eval(da1); 
		double da2Value = evaluator.eval(da2);
		
		if (da1Value > da2Value)
			return -1;
		else{
			if (da1Value < da2Value)
				return 1;			
			else
				return 0;
		}
	}

}
