package cilabo.gbml.problem.impl.multilabel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.fuzzy.knowledge.factory.HomoTriangleKnowledgeFactory;
import cilabo.fuzzy.knowledge.membershipParams.HomoTriangle_2_3_4_5;
import cilabo.fuzzy.rule.antecedent.Antecedent;
import cilabo.fuzzy.rule.antecedent.AntecedentFactory;
import cilabo.fuzzy.rule.antecedent.factory.HeuristicRuleGenerationMethod;
import cilabo.fuzzy.rule.consequent.Consequent;
import cilabo.fuzzy.rule.consequent.ConsequentFactory;
import cilabo.fuzzy.rule.consequent.factory.MultiLabel_MoFGBML_Learning;
import cilabo.gbml.objectivefunction.ObjectiveFunction;
import cilabo.gbml.objectivefunction.impl.MultiLabelOutputOfPittsburgh;
import cilabo.gbml.objectivefunction.impl.NumberOfRules;
import cilabo.gbml.problem.AbstractPitssburghGBML_Problem;
import cilabo.gbml.solution.MichiganSolution;
import cilabo.gbml.solution.PittsburghSolution;
import cilabo.main.Consts;
import cilabo.metric.Metric;
import cilabo.metric.multilabel.SubsetAccuracy;
import cilabo.utility.GeneralFunctions;
import cilabo.utility.Random;

public class MOP1_SubsetAccuracyAndRuleNum extends AbstractPitssburghGBML_Problem<IntegerSolution> {
	// ************************************
	private DataSet evaluationDataset;
	private float[][] params = HomoTriangle_2_3_4_5.getParams();

	// ************************************
	public MOP1_SubsetAccuracyAndRuleNum(DataSet train) {
		this.evaluationDataset = train;
		setNumberOfVariables(train.getNdim()*Consts.MAX_RULE_NUM);
		setNumberOfObjectives(2);
		setNumberOfConstraints(0);
		setName("MOP1_maxSubAcc_and_minNrule");

		// Initialization
		this.knowledge = HomoTriangleKnowledgeFactory.builder()
							.dimension(train.getNdim())
							.params(params)
							.build()
							.create();
		AntecedentFactory antecedentFactory = HeuristicRuleGenerationMethod.builder()
											.knowledge(knowledge)
											.train(train)
											.samplingIndex(new Integer[] {})
											.build();
		ConsequentFactory consequentFactory = MultiLabel_MoFGBML_Learning.builder()
												.train(train)
												.build();
		setAntecedentFactory(antecedentFactory);
		setConsequentFactory(consequentFactory);

		// Boundary
		List<Integer> lowerLimit = new ArrayList<>(getNumberOfVariables());
		List<Integer> upperLimit = new ArrayList<>(getNumberOfVariables());
		for(int i = 0; i < getNumberOfVariables(); i++) {
			lowerLimit.add(0);
			upperLimit.add(params.length);
		}
		setVariableBounds(lowerLimit, upperLimit);
	}

	// ************************************
	/* Getter */
	public DataSet getEvaluationDataset() {
		return this.evaluationDataset;
	}

	/* Setter */
	public void setEvaluationDataset(DataSet evaluationDataset) {
		this.evaluationDataset = evaluationDataset;
	}

	/* Method */
	@Override
	public PittsburghSolution createSolution() {
		// Boundary
		int dimension = evaluationDataset.getNdim();
		List<Integer> lowerBounds = new ArrayList<>(dimension);
		List<Integer> upperBounds = new ArrayList<>(dimension);
		for(int i = 0; i < dimension; i++) {
			lowerBounds.add(0);
			upperBounds.add(params.length);
		}
		List<Pair<Integer, Integer>> michiganBounds =
		        IntStream.range(0, lowerBounds.size())
		            .mapToObj(i -> new ImmutablePair<>(lowerBounds.get(i), upperBounds.get(i)))
		            .collect(Collectors.toList());

		// Rules
		Integer[] samplingIndex = GeneralFunctions.samplingWithout(evaluationDataset.getDataSize(), //box
																	Consts.INITIATION_RULE_NUM,	//want
																	Random.getInstance().getGEN());
		((HeuristicRuleGenerationMethod)antecedentFactory).setSamplingIndex(samplingIndex);
		List<IntegerSolution> michiganPopulation = new ArrayList<>();
		for(int i = 0; i < Consts.INITIATION_RULE_NUM; i++) {
			Antecedent antecedent = antecedentFactory.create();
			Consequent consequent = consequentFactory.learning(antecedent);

			MichiganSolution solution
				= new MichiganSolution(michiganBounds,
										1,	// Number of objectives for Michigan solution
					 					0,	// Number of constraints for Michigan solution
					 					antecedent, consequent);
			michiganPopulation.add(solution);
		}

		PittsburghSolution solution
			= new PittsburghSolution(this.getBounds(),
									 this.getNumberOfObjectives(),
									 michiganPopulation,
									 classification);
		return solution;
	}

	@Override
	public void evaluate(IntegerSolution solution) {
		MultiLabelOutputOfPittsburgh function = new MultiLabelOutputOfPittsburgh(evaluationDataset);
		ArrayList<ClassLabel> classifiedClassLabels = function.function((PittsburghSolution)solution);

		/* The first objective */
		Metric function1 = new SubsetAccuracy();
		double f1 = (double)function1.metric(classifiedClassLabels, evaluationDataset);

		/* The second objective */
		ObjectiveFunction<PittsburghSolution, Double> function2 = new NumberOfRules();
		double f2 = function2.function((PittsburghSolution)solution);

		solution.setObjective(0, -1.0*f1);
		solution.setObjective(1, f2);
	}

}
