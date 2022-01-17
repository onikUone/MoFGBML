package cilabo.labo.developing.multitasking;

import java.util.List;

import org.uma.jmetal.solution.Solution;

public interface TaskManager {
	public void initTaskList();
	public List<Task<? extends Solution<?>>> getTaskList();
}
