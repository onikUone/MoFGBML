package cilabo.util.fileoutput;

import java.util.List;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.fileoutput.FileOutputContext;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public abstract class SolutionListOutputFormat {
	// ************************************************************
	protected List<Boolean> isObjectiveToBeMinimized;

	// ************************************************************

	// ************************************************************
	public void print(String fileName, List<? extends Solution<?>> solutionList) {
		FileOutputContext fileContext = new DefaultFileOutputContext(fileName);
		if(isObjectiveToBeMinimized == null) {
			printSolutionList(fileContext, solutionList);
		}
		else {
			printSolutionList(fileContext, solutionList);
		}
	}

	public abstract void printSolutionList(FileOutputContext fileContext, List<? extends Solution<?>> solutionList);

	/* Setter */
	public SolutionListOutputFormat setObjectiveMinimizingObjectiveList(
			List<Boolean> isObjectiveToBeMinimized)
	{
		this.isObjectiveToBeMinimized = isObjectiveToBeMinimized;
		return this;
	}

}
