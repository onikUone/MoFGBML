package cilabo.labo.developing.multitasking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.component.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.component.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.fileoutput.FileOutputContext;

import cilabo.gbml.solution.util.attribute.multitasking.BirthPlace;
import cilabo.gbml.solution.util.attribute.multitasking.FamilyLine;
import cilabo.gbml.solution.util.attribute.multitasking.ParentOrChild;
import cilabo.util.fileoutput.SolutionListOutputFormat;

public class MultiTaskingSolutionListOutput extends SolutionListOutputFormat {
	private TaskManager taskManager;

	public MultiTaskingSolutionListOutput(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	/* Setter */
	public MultiTaskingSolutionListOutput setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
		return this;
	}

	@Override
	public void printSolutionList(FileOutputContext fileContext, List<? extends Solution<?>> solutionList) {
		String ln = System.lineSeparator();

		BufferedWriter bufferedWriter = fileContext.getFileWriter();
		try {
			if(solutionList.size() > 0) {
				String str = "";
				int numberOfObjectives = solutionList.get(0).getNumberOfObjectives();
				int numberOfTasks = taskManager.getTaskList().size();

				/* Header */
				//id
				str = "id";
				//fitness
				for(int i = 0; i < numberOfObjectives; i++) {
					str += "," + "f" + String.valueOf(i);
				}
				//NSGA-II
				str += "," + "rank";
				str += "," + "crowding";
				//Multi-Tasking
				str += "," + "current";
				for(int i = 0; i < numberOfTasks; i++) {
					str += "," + "blood_task"+String.valueOf(i+1);
				}
				str += "," + "birthPlace";
				str += ln;
				bufferedWriter.write(str);

				/* Body of solutions */
				for(int i = 0; i < solutionList.size(); i++) {
					str = "";
					Solution<?> solution = solutionList.get(i);

					//id
					str = String.valueOf(i);
					//fitness
					for(int o = 0; o < numberOfObjectives; o++) {
						str += "," + solution.getObjective(o);
					}
					//NSGA-II
					// rank
					str += "," + solution.getAttribute((new FastNonDominatedSortRanking<>()).getAttributeId());
					// crowding distance
					str += "," + solution.getAttribute((new CrowdingDistanceDensityEstimator<>()).getAttributeId());
					//Multi-Tasking
					// current
					str += "," + solution.getAttribute((new ParentOrChild<>()).getAttributeId());
					// family line
					@SuppressWarnings("unchecked")
					Map<String, Double> familyLine = (Map<String, Double>)solution.getAttribute((new FamilyLine<>()).getAttributeId());
					for(int t = 0; t < numberOfTasks; t++) {
						double value = familyLine.get("task"+(t+1));
						str += "," + value;
					}
					// birth place
					str += "," + solution.getAttribute((new BirthPlace<>()).getAttributeId());
					str += ln;
					bufferedWriter.write(str);
				}
				bufferedWriter.close();
			}
		}
		catch (IOException e) {
			throw new JMetalException("Error writing data ", e);
		}

	}


}
