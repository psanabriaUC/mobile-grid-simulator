package edu.isistan.baseprofile.usersession.time;

import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

/**This is a simplified version of the weibull distribution used to model the time between
 * user sessions proposed in the paper "Diversity in smartphone usage".*/
public class WeibullSessionTime {
	
	/**Due to the lack of the function inferred in the paper "Diversity in smartphone usage"
	 * that describes the scale values of the weibull distribution, the values of such
	 * function were discretized using the EnumeratedRealDistribution. 
	 * The samples of that distribution are used to instantiate the scale parameter of the
	 * weibull distribution*/
	public static EnumeratedRealDistribution scaleDistribution = new EnumeratedRealDistribution(new double[]{75d,175d,325d,400d}, new double[]{0.25d,0.25d,0.25d,0.25d});
	
	private double scale;
	
	/**Due to the lack of the function inferred in the paper "Diversity in smartphone usage"
	 * that describes the shape values for the weibull distribution, the values of such
	 * function were discretized using the EnumeratedRealDistribution. 
	 * The samples of that distribution are used to instantiate the shape parameter of the
	 * weibull distribution*/
	public static EnumeratedRealDistribution shapeDistribution = new EnumeratedRealDistribution(new double[]{0.25d,0.3d,0.4d,0.45d}, new double[]{0.08d,0.5d,0.20d,0.22d});
	
	private double shape;
	
	private WeibullDistribution timeBtwSessionDistribution;
	
	public WeibullSessionTime(){
		scale = scaleDistribution.sample();
		shape = shapeDistribution.sample();
		
		timeBtwSessionDistribution = new WeibullDistribution(shape,scale);
	}
	
	/**returns a sample of the weibull distribution. The unit of the value returned is in seconds**/
	public double getTimeBtwSessionSample(){
		return timeBtwSessionDistribution.sample();
	}
	
	/**returns an array of samples of the weibull distribution. The unit of the value returned is in seconds**/
	public double[] getTimeBtwSessionSamples(int cant){			
		return timeBtwSessionDistribution.sample(cant);
	}
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName()+"(scale="+scale+"_shape="+shape+")";
	}
	
}
