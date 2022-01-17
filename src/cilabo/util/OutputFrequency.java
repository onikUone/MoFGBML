package cilabo.util;

import java.util.Map;

public interface OutputFrequency {
	public boolean isTimeToOutput(Map<String, Object> algorithmStatusData);
}
