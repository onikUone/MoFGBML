package cilabo.labo.developing.multitasking;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.component.selection.MatingPoolSelection;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.Solution;

import cilabo.gbml.operator.selection.FirstObjectiveOrientedSelection;

public class SharingSolutionInformationSelection<S extends Solution<?>>
		implements MatingPoolSelection<S>
{
	// ************************************************************
	private String ownTaskLabel;
	private SelectionOperator<List<S>, S> selectionInTask;
	private int matingPoolSize;
	private TaskManager taskManager;


	// ************************************************************
	public SharingSolutionInformationSelection(String ownTaskLabel,
											   SelectionOperator<List<S>, S> selectionInTask,
											   TaskManager taskManager,
											   int matingPoolSize)
	{
		this.ownTaskLabel = ownTaskLabel;

		this.selectionInTask = selectionInTask;

		this.taskManager = taskManager;
		this.matingPoolSize = matingPoolSize;
	}

	// ************************************************************
	@SuppressWarnings("unchecked")
	public List<S> select(List<S> solutionList) {
		List<S> matingPool = new ArrayList<>(matingPoolSize);

		int numberOfTask = taskManager.getTaskList().size();
		int offspringPopulatinSizePerTask = CommandLineArgs.sharingAmount / (numberOfTask - 1);

		Task<? extends Solution<?>> ownTask = null;
		for(int i = 0; i < numberOfTask; i++) {
			Task<? extends Solution<?>> task = taskManager.getTaskList().get(i);
			if(task.getTaskLabel().equals(this.ownTaskLabel)) {
				ownTask = task;
				break;
			}
		}

		/* タスク間交叉親個体選択 */
		FirstObjectiveOrientedSelection<S> selectionOutTask
			= new FirstObjectiveOrientedSelection<>(offspringPopulatinSizePerTask);
		for(int i = 0; i < numberOfTask; i++) {
			Task<? extends Solution<?>> task = taskManager.getTaskList().get(i);
			if(task.getTaskLabel().equals(this.ownTaskLabel)) {
				continue;
			}
			else {
				List<S> selectedSolutions = selectionOutTask.execute((List<S>)task.getPopulation());
				for(int j = 0; j < selectedSolutions.size(); j++) {
					// 一方の親は他タスクから選択
					matingPool.add(selectedSolutions.get(j));
					// もう一方の親は自タスクから選択
					matingPool.add(selectionInTask.execute((List<S>)ownTask.getPopulation()));
				}
			}
		}

		/* タスク間交叉親個体選択 */
		while(matingPool.size() < matingPoolSize) {
			if(ownTask != null) {
				matingPool.add(selectionInTask.execute((List<S>) ownTask.getPopulation()));
			}
		}

		return matingPool;
	}


}
