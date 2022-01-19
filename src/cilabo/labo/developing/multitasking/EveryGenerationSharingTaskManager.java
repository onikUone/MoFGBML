package cilabo.labo.developing.multitasking;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.algorithm.impl.AbstractEvolutionaryAlgorithm;
import org.uma.jmetal.component.densityestimator.DensityEstimator;
import org.uma.jmetal.component.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.component.replacement.Replacement;
import org.uma.jmetal.component.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.selection.MatingPoolSelection;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.variation.Variation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.ObservableEntity;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

import cilabo.data.DataSet;
import cilabo.fuzzy.classifier.operator.classification.Classification;
import cilabo.gbml.operator.crossover.HybridGBMLcrossover;
import cilabo.gbml.operator.crossover.MichiganOperation;
import cilabo.gbml.operator.crossover.PittsburghCrossover;
import cilabo.gbml.operator.mutation.PittsburghMutation;
import cilabo.gbml.problem.AbstractPitssburghGBML_Problem;
import cilabo.gbml.problem.impl.multilabel.MOP1_SubsetAccuracyAndRuleNum;
import cilabo.gbml.problem.impl.multilabel.MOP2_HammingLossAndRuleNum;
import cilabo.gbml.problem.impl.multilabel.MOP3_FmeasureAndRuleNum;
import cilabo.main.Consts;
import cilabo.util.OutputFrequency;
import cilabo.util.fileoutput.SolutionListOutputFormat;
import cilabo.utility.Output;

public class EveryGenerationSharingTaskManager<S extends Solution<?>>
							extends AbstractEvolutionaryAlgorithm<S, List<S>>
							implements TaskManager, ObservableEntity {
	// ************************************************************
	protected DataSet train;
	protected List<Task<? extends Solution<?>>> taskList;
	protected int sharingAmount;

	private int evaluations;
	private int populationSize;
	private int offspringPopulationSize;
	private OutputFrequency outputFrequency;
	private String outputRootDir;

	private Map<String, Object> algorithmStatusData;
	private Observable<Map<String, Object>> observable;

	Termination termination;
	Classification classification;
	private long startTime;
	private long totalComputingTime;

	// ************************************************************
	/* Constructor */
	public EveryGenerationSharingTaskManager(DataSet train,
											 int sharingAmount,
											 int populationSize,
											 int offspringPopulationSize,
											 OutputFrequency outputFrequency,
											 String outputRootDir,
											 Termination termination,
											 Classification classification)
	{
		this.train = train;
		this.sharingAmount = sharingAmount;

		this.populationSize = populationSize;
		this.offspringPopulationSize = offspringPopulationSize;
		this.outputFrequency = outputFrequency;
		this.outputRootDir = outputRootDir;

		this.termination = termination;
		this.classification = classification;

		this.algorithmStatusData = new HashMap<>();
		this.observable = new DefaultObservable<>("MultiTasking Hybrid MoFGBML with NSGA-II algorithm");

	}

	// ************************************************************

	@Override
	protected void initProgress() {
		evaluations = populationSize;

	    algorithmStatusData.put("EVALUATIONS", evaluations);
	    algorithmStatusData.put("POPULATION", population);
	    algorithmStatusData.put("COMPUTING_TIME", System.currentTimeMillis() - startTime);

	    observable.setChanged();
	    observable.notifyObservers(algorithmStatusData);
	}

	@Override
	protected void updateProgress() {
		evaluations += offspringPopulationSize;
	    algorithmStatusData.put("EVALUATIONS", evaluations);
	    algorithmStatusData.put("POPULATION", population);
	    algorithmStatusData.put("COMPUTING_TIME", System.currentTimeMillis() - startTime);

	    observable.setChanged();
	    observable.notifyObservers(algorithmStatusData);

	    Integer evaluations = (Integer)algorithmStatusData.get("EVALUATIONS");
	    if(evaluations % (Consts.PER_SHOW_DOT * Consts.populationSize) == 0) {
	    	System.out.println("Evaluation: " + evaluations);
	    }

	}

	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		/* === START === */

		/* 各種タスク生成 */
		initTaskList();
		/* 出身タスクAttribute (空 - empty) */
		Map<String, Double> familyLine = new HashMap<>();
		for(int i = 0; i < taskList.size(); i++) {
			familyLine.put(taskList.get(i).getTaskLabel(), 0.0);
		}

		/* 初期化 */
		taskList.stream().forEach(task -> task.runInitialize(familyLine));
		/* JMetal progress initialization */
		initProgress();

		while(!isStoppingConditionReached()) {
			/* 親個体選択 - Mating Selection */
			taskList.stream().forEach(task -> task.runSelection());
			/* 1世代更新 - Runnin a generation */
			taskList.stream().forEach(task -> task.runSingleGeneration());
			/* JMetal progress update */
			updateProgress();
		}

		/* ===  END  === */
		totalComputingTime = System.currentTimeMillis() - startTime;
	}

	@Override
	public void initTaskList() {
		this.taskList = new ArrayList<>();

		/* MOP1: maximize Subset Accuracy and minimize Number of rules */
		AbstractPitssburghGBML_Problem<IntegerSolution> mop1 = new MOP1_SubsetAccuracyAndRuleNum(train);
		mop1.setClassification(classification);
		taskList.add(makeTask(mop1, "task1"));

		/* MOP2: minimize Hamming Loss and minimize Number of rules */
		AbstractPitssburghGBML_Problem<IntegerSolution> mop2 = new MOP2_HammingLossAndRuleNum(train);
		mop2.setClassification(classification);
		taskList.add(makeTask(mop2, "task2"));

		/* MOP3: maximize F-Measure and minimize Number of rules */
		AbstractPitssburghGBML_Problem<IntegerSolution> mop3 = new MOP3_FmeasureAndRuleNum(train);
		mop3.setClassification(classification);
		taskList.add(makeTask(mop3, "task3"));
	}

	private Task<? extends Solution<?>> makeTask(AbstractPitssburghGBML_Problem<IntegerSolution> problem, String label) {
		String sep = File.separator;

		String taskDir = outputRootDir+sep+label;
		Output.mkdirs(taskDir);

		Task<IntegerSolution> task = new Task<>(problem,
												label,
												populationSize,
												offspringPopulationSize,
												outputFrequency,
												taskDir);

		/* Crossover: Hybrid-style GBML specific crossover operator. */
		double crossoverProbability = 1.0;
		/* Michigan operation */
		CrossoverOperator<IntegerSolution> michiganX = new MichiganOperation(Consts.MICHIGAN_CROSS_RT,
																			 problem.getKnowledge(),
																			 problem.getConsequentFactory());
		/* Pittsburgh operation */
		CrossoverOperator<IntegerSolution> pittsburghX = new PittsburghCrossover(Consts.PITTSBURGH_CROSS_RT);
		/* Hybrid-style crossover */
		CrossoverOperator<IntegerSolution> crossover = new HybridGBMLcrossover(crossoverProbability, Consts.MICHIGAN_OPE_RT,
																				michiganX, pittsburghX);
		task.setCrossoverOperator(crossover);

		/* Mutation: Pittsburgh-style GBML specific mutation operator. */
		MutationOperator<IntegerSolution> mutation = new PittsburghMutation(problem.getKnowledge(), train);
		task.setMutationOperator(mutation);

		/* Variation: */
		Variation<IntegerSolution> variation =
				new MultiTaskingVariation<>(
						label,
						offspringPopulationSize,
						crossover,
						mutation,
						problem.getConsequentFactory());
		task.setVariation(variation);

		/* Replacement: NSGA-II */
		DensityEstimator<IntegerSolution> densityEstimator = new CrowdingDistanceDensityEstimator<>();
		Ranking<IntegerSolution> ranking = new FastNonDominatedSortRanking<>();
		Replacement<IntegerSolution> replacement =
				new RankingAndDensityEstimatorReplacement<>(
						ranking, densityEstimator, Replacement.RemovalPolicy.oneShot);
		task.setReplacement(replacement);

		/* Selection: */
		int matingPoolSize = offspringPopulationSize *
				crossover.getNumberOfRequiredParents() / crossover.getNumberOfGeneratedChildren();
		/* Selection for the Crossover "in" Task - タスク内交叉用親個体選択 */
		SelectionOperator<List<IntegerSolution>, IntegerSolution> selectionInTask
			= new NaryTournamentSelection<>(
				2,	// Tournament Size;
				new MultiComparator<>(
					Arrays.asList(
					ranking.getSolutionComparator(), densityEstimator.getSolutionComparator()))
				);
		/* Selection for the Crossover "out" Task - タスク間交叉用親個体選択 */
		MatingPoolSelection<IntegerSolution> selection
			= new SharingSolutionInformationSelection<IntegerSolution>(label,
																	   selectionInTask,
																	   this,
																	   matingPoolSize);
		task.setSelection(selection);

		/* SolutionListOutput */
		SolutionListOutputFormat solutionListOutput = new MultiTaskingSolutionListOutput(this);
		task.setSolutionListOutput(solutionListOutput);
		return task;
	}

	@Override
	public String getName() {
		return "Multitasking multiobjective optimization";
	}

	@Override
	public String getDescription() {
		return "Multitasking Multiobjective Fuzzy Genetics-Based Machine Learning with NSGA-II";
	}

	/* Getter */
	public int getSharingAmount() {
		return this.sharingAmount;
	}

	/* Getter */
	public long getTotalComputingTime() {
		return this.totalComputingTime;
	}

	/* Getter */
	@Override
	public List<Task<? extends Solution<?>>> getTaskList() {
		return this.taskList;
	}

	/* Setter */
	public EveryGenerationSharingTaskManager<S> setTrain(DataSet train) {
		this.train = train;
		return this;
	}

	/* Setter */
	public EveryGenerationSharingTaskManager<S> setSharingAmount(int sharingAmount) {
		this.sharingAmount = sharingAmount;
		return this;
	}

	@Override
	public Observable<Map<String, Object>> getObservable() {
		return this.observable;
	}

	@Override
	protected boolean isStoppingConditionReached() {
		return this.termination.isMet(algorithmStatusData);
	}

	@Override
	protected List<S> createInitialPopulation() {
		// 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		// 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected List<S> selection(List<S> population) {
		// 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected List<S> reproduction(List<S> population) {
		// 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
		// 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public List<S> getResult() {
		// 自動生成されたメソッド・スタブ
		return null;
	}

}
