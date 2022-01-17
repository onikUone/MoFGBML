package cilabo.util;

import java.util.Map;

import org.uma.jmetal.util.JMetalLogger;

public class ConstantPeriodOutput implements OutputFrequency {
	private int frequency;

	public ConstantPeriodOutput(int frequency) {
		this.frequency = frequency;
	}


	@Override
	public boolean isTimeToOutput(Map<String, Object> algorithmStatusData) {
		Integer evaluations = (Integer)algorithmStatusData.get("EVALUATIONS");
		if(evaluations != null) {
			if(evaluations % frequency == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			JMetalLogger.logger.warning(getClass().getName()
			+ ": The algorithm has not registered yet any info related to the EVALUATIONS key");
			return false;
		}
	}
}
