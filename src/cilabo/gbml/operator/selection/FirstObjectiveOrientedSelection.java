package cilabo.gbml.operator.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.checking.Check;

public class FirstObjectiveOrientedSelection<S extends Solution<?>>
	implements SelectionOperator<List<S>, List<S>>
{
	// ************************************************************
	private int numberOfSolutionsToBeReturned;

	// ************************************************************
	/** Constructor */
	public FirstObjectiveOrientedSelection() {
		this(1);
	}

	/** Constructor */
	public FirstObjectiveOrientedSelection(int numberOfSolutionsToBeReturned) {
		this.numberOfSolutionsToBeReturned = numberOfSolutionsToBeReturned;
	}

	// ************************************************************

	/* Setter */
	public FirstObjectiveOrientedSelection<S> setNumberOfSolutionsToBeReturned(int numberOfSolutionsToBeReturned) {
		this.numberOfSolutionsToBeReturned = numberOfSolutionsToBeReturned;
		return this;
	}

	@Override
	/** Execute() method */
	public List<S> execute(List<S> solutionList) {
		Check.isNotNull(solutionList);
		Check.collectionIsNotEmpty(solutionList);
		Check.that(
				solutionList.size() >= numberOfSolutionsToBeReturned,
				"The solution list size ("
					+ solutionList.size()
					+ ") is less than "
					+ "the number of requested solutions ("
					+ numberOfSolutionsToBeReturned
					+ ")");

		/* === START === */
		List<S> result = new ArrayList<>();
		if(solutionList.size() == 1) {
			result.add(solutionList.get(0));
		}
		else {
			/* Sort population by the First objective value */
			List<S> sortedList = solutionList.stream()
					.sorted(new Comparator<S>() {
						@Override
						public int compare(S p1, S p2) {
							double f1_p1 = p1.getObjective(0);
							double f1_p2 = p2.getObjective(0);

							if(f1_p1 > f1_p2) {
								return 1;
							}
							else if(f1_p1 < f1_p2) {
								return -1;
							}
							else {
								return 0;
							}
						}
					}
					).collect(Collectors.toList());
			for(int i = 0; i < numberOfSolutionsToBeReturned; i++) {
				result.add(sortedList.get(i));
			}
		}
		return result;
	}

}
