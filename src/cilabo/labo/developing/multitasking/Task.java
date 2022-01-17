package cilabo.labo.developing.multitasking;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.algorithm.impl.AbstractEvolutionaryAlgorithm;
import org.uma.jmetal.component.evaluation.Evaluation;
import org.uma.jmetal.component.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.initialsolutioncreation.InitialSolutionsCreation;
import org.uma.jmetal.component.initialsolutioncreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.replacement.Replacement;
import org.uma.jmetal.component.selection.MatingPoolSelection;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.variation.Variation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.ObservableEntity;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

import cilabo.gbml.solution.util.attribute.multitasking.FamilyLine;
import cilabo.gbml.solution.util.attribute.multitasking.ParentOrChild;
import cilabo.util.OutputFrequency;
import cilabo.util.fileoutput.SolutionListOutputFormat;

public class Task<S extends Solution<?>> extends AbstractEvolutionaryAlgorithm<S, List<S>>
										implements ObservableEntity
{
	// ************************************************************
	private String taskLabel;
	private int evaluations;
	private int populationSize;
	private int offspringPopulationSize;
	private OutputFrequency outputFrequency;
	private String outputRootDir;

	protected SelectionOperator<List<S>, S> selectionOperator;
	protected CrossoverOperator<S> crossoverOperator;
	protected MutationOperator<S> mutationOperator;
	private Termination termination;
	private Variation<S> variation;
	private InitialSolutionsCreation<S> initialSolutionsCreation;

	private Map<String, Object> algorithmStatusData;

	private Evaluation<S> evaluation;
	private Replacement<S> replacement;
	private MatingPoolSelection<S> selection;

	private long startTime;
	private long totalComputingTime;

	private Observable<Map<String, Object>> observable;

	SolutionListOutputFormat solutionListOutput;

	// ************************************************************
	/** Constructor */
	public Task(
			/* Arguments */
			Problem<S> problem,
			String taskLabel,
			int populationSize,
			int offspringPopulationSize,
			OutputFrequency outputFrequency,
			String outputRootDir)
	{
		/* Body */
		this.problem = problem;

		this.taskLabel = taskLabel;
		this.populationSize = populationSize;
		this.offspringPopulationSize = offspringPopulationSize;
		this.outputFrequency = outputFrequency;
		this.outputRootDir = outputRootDir;

		/* Initialization */
		this.initialSolutionsCreation = new RandomSolutionsCreation<>(problem, populationSize);
		this.evaluation = new SequentialEvaluation<>();

		this.algorithmStatusData = new HashMap<>();
		this.observable = new DefaultObservable<>("A Task for MultiTasking Hybrid MoFGBML with NSGA-II algorithm");
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

	    /* Output initial population */
	    Integer evaluations = (Integer)algorithmStatusData.get("EVALUATIONS");
	    String sep = File.separator;
	    String path = outputRootDir+sep+ "solutions-"+evaluations+".txt";
	    this.solutionListOutput.print(path, getPopulation());
	}

	@Override
	protected void updateProgress() {
		evaluations += offspringPopulationSize;
	    algorithmStatusData.put("EVALUATIONS", evaluations);
	    algorithmStatusData.put("POPULATION", population);
	    algorithmStatusData.put("COMPUTING_TIME", System.currentTimeMillis() - startTime);

	    observable.setChanged();
	    observable.notifyObservers(algorithmStatusData);

	    /* Output initial population */
	    Integer evaluations = (Integer)algorithmStatusData.get("EVALUATIONS");
	    if(evaluations != null) {
	    	if(this.outputFrequency.isTimeToOutput(algorithmStatusData)) {
	    		String sep = File.separator;
	    		String path = outputRootDir+sep+ "solutions-"+evaluations+".txt";
	    		this.solutionListOutput.print(path, getPopulation());
	    	}
	    }
		else {
			JMetalLogger.logger.warning(getClass().getName()
			+ ": The algorithm has not registered yet any info related to the EVALUATIONS key");
		}
	}

	/**
	 * 初期化実行
	 */
	public void runInitialize(Map<String, Double> familyLine) {
		long startTime = System.currentTimeMillis();

		/* ============================================================ */
		/* Step 1. 初期個体群生成 - Initialization Population */
		population = createInitialPopulation();
		/* Step 2. 出身タスクAttribute付与 */
		population.stream().forEach(solution -> {
			Map<String, Double> attribute = new HashMap<>(familyLine);	// Deep Copy
			attribute.put(this.taskLabel, 1.0);
			/* Family Line */
			solution.setAttribute((new FamilyLine<>()).getAttributeId(), attribute);
			/* How to be generated */
			solution.setAttribute((new ParentOrChild<>()).getAttributeId(), "current");
		});
		/* Step 3. 初期個体群評価 - Initial Population Evaluation */
		population = evaluatePopulation(population);
		/* JMetal progress initialization */
		initProgress();
		/* ============================================================ */

		totalComputingTime += System.currentTimeMillis() - startTime;
	}

	/**
	 * Mating Population 選択
	 */
	public void runSelection() {
		long startTime = System.currentTimeMillis();

		List<S> matingPopulation;
		/* 親個体選択 - Mating Selection */
		matingPopulation = selection(population);
		algorithmStatusData.put("MATING_POPULATION", matingPopulation);
		totalComputingTime += System.currentTimeMillis() - startTime;
	}

	/**
	 * 1世代分の実行
	 */
	public void runSingleGeneration() {
		long startTime = System.currentTimeMillis();

		@SuppressWarnings("unchecked")
		List<S> matingPopulation = (List<S>)algorithmStatusData.get("MATING_POPULATION");
		List<S> offspringPopulation;
		/* ============================================================ */
		/* 子個体群生成 - Offspring Generation */
		offspringPopulation = reproduction(matingPopulation);
		/* 子個体群評価 - Offsprign Evaluation */
		offspringPopulation = evaluatePopulation(offspringPopulation);
		/* 個体群更新・環境選択 - Environmental Selection */
		population = replacement(population, offspringPopulation);
		/* 次世代個体群 Attribute更新 */
		population.stream().forEach(solution -> {
			solution.setAttribute((new ParentOrChild<>()).getAttributeId(), "current");
		});

		/* JMetal progress update */
		updateProgress();
		/* ============================================================ */

		totalComputingTime += System.currentTimeMillis() - startTime;
	}

	// ************************************************************
	@Override
	protected List<S> createInitialPopulation() {
		return this.initialSolutionsCreation.create();
	}

	@Override
	protected boolean isStoppingConditionReached() {
		return this.termination.isMet(algorithmStatusData);
	}

	@Override
	protected List<S> selection(List<S> population) {
		return this.selection.select(population);
	}

	@Override
	protected List<S> reproduction(List<S> matingPool) {
		return this.variation.variate(population, matingPool);
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		return this.evaluation.evaluate(population, getProblem());
	}

	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
		return this.replacement.replace(population, offspringPopulation);
	}

	@Override
	public List<S> getResult() {
		return SolutionListUtils.getNonDominatedSolutions(getPopulation());
	}

	@Override
	public Observable<Map<String, Object>> getObservable() {
		return this.observable;
	}

	@Override
	public String getName() {
		return "A task for multitasking multiobjective optimization";
	}

	@Override
	public String getDescription() {
		return "A task of Multitasking Multiobjective Fuzzy Genetics-Based Machine Learning with NSGA-II";
	}

	// ************************************************************
	/* Getter */
	public int getEvaluations() {
		return this.evaluations;
	}

	/* Getter */
	public String getTaskLabel() {
		return this.taskLabel;
	}

	/* Getter */
	public int getPopulationSize() {
		return this.populationSize;
	}

	/* Getter */
	public int getOffspringPopulationSize() {
		return this.offspringPopulationSize;
	}

	/* Getter */
	public OutputFrequency getOutputFrequency() {
		return this.outputFrequency;
	}

	/* Getter */
	public String getOutputRootDir() {
		return this.outputRootDir;
	}

	/* Getter */
	public Map<String, Object> getAlgorithmStatusData() {
		return this.algorithmStatusData;
	}

	/* Getter */
	public long getStartTime() {
		return this.startTime;
	}

	/* Getter */
	public long getTotalComputingTime() {
		return this.totalComputingTime;
	}

	// ************************************************************
	/* Setter */
	public Task<S> setSelectionOperator(SelectionOperator<List<S>, S> selectionOperator) {
		this.selectionOperator = selectionOperator;
		return this;
	}

	/* Setter */
	public Task<S> setCrossoverOperator(CrossoverOperator<S> crossoverOperator) {
		this.crossoverOperator = crossoverOperator;
		return this;
	}

	/* Setter */
	public Task<S> setMutationOperator(MutationOperator<S> mutationOperator) {
		this.mutationOperator = mutationOperator;
		return this;
	}

	/* Setter */
	public Task<S> setTermination(Termination termination) {
		this.termination = termination;
		return this;
	}

	/* Setter */
	public Task<S> setVariation(Variation<S> variation) {
		this.variation = variation;
		return this;
	}

	/* Setter */
	public Task<S> setInitialSolutionsCreation(InitialSolutionsCreation<S> initialSolutionsCreation) {
		this.initialSolutionsCreation = initialSolutionsCreation;
		return this;
	}

	/* Setter */
	public Task<S> setEvaluation(Evaluation<S> evaluation) {
		this.evaluation = evaluation;
		return this;
	}

	/* Setter */
	public Task<S> setReplacement(Replacement<S> replacement) {
		this.replacement = replacement;
		return this;
	}

	/* Setter */
	public Task<S> setSelection(MatingPoolSelection<S> selection) {
		this.selection = selection;
		return this;
	}

	/* Setter */
	public Task<S> setStarTime(long startTime) {
		this.startTime = startTime;
		return this;
	}

	/* Setter */
	public Task<S> setTotalComputingTime(long totalComputingTime) {
		this.totalComputingTime = totalComputingTime;
		return this;
	}

	/* Setter */
	public Task<S> setSolutionListOutput(SolutionListOutputFormat solutionListOutput) {
		this.solutionListOutput = solutionListOutput;
		return this;
	}



}
