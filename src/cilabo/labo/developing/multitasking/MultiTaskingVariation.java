package cilabo.labo.developing.multitasking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.uma.jmetal.component.variation.Variation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalException;

import cilabo.fuzzy.rule.antecedent.Antecedent;
import cilabo.fuzzy.rule.consequent.Consequent;
import cilabo.fuzzy.rule.consequent.ConsequentFactory;
import cilabo.gbml.solution.MichiganSolution;
import cilabo.gbml.solution.PittsburghSolution;
import cilabo.gbml.solution.util.attribute.multitasking.FamilyLine;
import cilabo.gbml.solution.util.attribute.multitasking.ParentOrChild;

/**
 * マルチタスク用のAttributeを付与する
 *
 */
public class MultiTaskingVariation<S extends Solution<?>>
			implements Variation<S>
{
	// ************************************************************
	private CrossoverOperator<S> crossover;
	private MutationOperator<S> mutation;
	private int matingPoolSize;
	private int offspringPopulationSize;
	private ConsequentFactory consequentFactory;

	// ************************************************************
	/* Constructor */
	public MultiTaskingVariation(
			/* Arguments */
			int offspringPopulationSize,
			CrossoverOperator<S> crossover,
			MutationOperator<S> mutation,
			ConsequentFactory consequentFactory)
	{
		/* Body */
		this.offspringPopulationSize = offspringPopulationSize;
		this.crossover = crossover;
		this.mutation = mutation;
		this.consequentFactory = consequentFactory;

		this.matingPoolSize = offspringPopulationSize *
				crossover.getNumberOfRequiredParents() / crossover.getNumberOfGeneratedChildren();

		int remainder = matingPoolSize % crossover.getNumberOfRequiredParents();
		if(remainder != 0) {
			matingPoolSize += remainder;
		}
	}

	// ************************************************************

	@SuppressWarnings("unchecked")
	@Override
	public List<S> variate(List<S> population, List<S> matingPopulation) {
		int numberOfParents = crossover.getNumberOfRequiredParents();

		checkNumberOfParents(matingPopulation, numberOfParents);

		List<S> offspringPopulation = new ArrayList<>(offspringPopulationSize);
		for(int i = 0; i < matingPoolSize; i+= numberOfParents) {
			List<S> parents = new ArrayList<>(numberOfParents);
			for(int j = 0; j < numberOfParents; j++) {
				parents.add(matingPopulation.get(i + j));
			}

			/* Crossover */
			List<S> offspring = crossover.execute(parents);

			for(S solution : offspring) {
				/* Mutation */
				mutation.execute(solution);
				/* Learning */
				List<IntegerSolution> michiganPopulation = ((PittsburghSolution)solution).getMichiganPopulation();
				List<IntegerSolution> newMichiganPopulation = new ArrayList<>();
				for(int j = 0; j < michiganPopulation.size(); j++) {
					Antecedent antecedent = ((MichiganSolution)michiganPopulation.get(j)).getRule().getAntecedent().deepcopy();
					Consequent consequent = consequentFactory.learning(antecedent);
					newMichiganPopulation.add(new MichiganSolution(
							((MichiganSolution)michiganPopulation.get(j)).getBounds(),
							((MichiganSolution)michiganPopulation.get(j)).getNumberOfObjectives(),
							((MichiganSolution)michiganPopulation.get(j)).getNumberOfConstraints(),
							antecedent, consequent));
				}
				((PittsburghSolution)solution).setMichiganPopulation(newMichiganPopulation);

				offspringPopulation.add(solution);
				if(offspringPopulation.size() == offspringPopulationSize) {
					break;
				}
			}

			// FamilyLine値計算，付与
			Map<String, Double> newFamilyLine = new HashMap<>();
			Set<String> keys = ((Map<String, Double>)parents.get(0).getAttribute((new FamilyLine<>()).getAttributeId())).keySet();
			for(String key : keys) {
				double sum = 0;
				for(int j = 0; j < numberOfParents; j++) {
					double value = ((Map<String, Double>)parents.get(j).getAttribute((new FamilyLine<>()).getAttributeId())).get(key);
					sum += value;
				}
				newFamilyLine.put(key, sum/(double)numberOfParents);
			}
			for(S solution : offspring) {
				solution.setAttribute((new FamilyLine<>()).getAttributeId(), newFamilyLine);
			}

			//タスク間交叉子個体 or タスク内交叉子個体 Attribute 付与
			if(offspringPopulation.size() <= CommandLineArgs.sharingAmount) {
				for(S solution : offspring) {
					solution.setAttribute((new ParentOrChild<>()).getAttributeId(), "Sharing");
				}
			}
			else {
				for(S solution : offspring) {
					solution.setAttribute((new ParentOrChild<>()).getAttributeId(), "Domestic");
				}
			}

		}
		return offspringPopulation;
	}

	/**
	 * A crossover operator is applied to a number of parents, and it assumed that the population contains
	 * a valid number of population. This method checks that.
	 * @param population
	 * @param numberOfParentsForCrossover
	 */
	private void checkNumberOfParents(List<S> population, int numberOfParentsForCrossover) {
		if ((population.size() % numberOfParentsForCrossover) != 0) {
			throw new JMetalException(
					"Wrong number of parents: the remainder if the " +
					"population size (" + population.size() + ") is not divisible by " +
					numberOfParentsForCrossover);
		}
	}

	@Override
	public int getMatingPoolSize() {
		return this.matingPoolSize;
	}

	@Override
	public int getOffspringPopulationSize() {
		return this.offspringPopulationSize;
	}
}
