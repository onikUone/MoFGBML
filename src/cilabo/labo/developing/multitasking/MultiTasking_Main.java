package cilabo.labo.developing.multitasking;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.algorithm.impl.AbstractEvolutionaryAlgorithm;
import org.uma.jmetal.component.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.component.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import cilabo.data.DataSet;
import cilabo.data.impl.TrainTestDatasetManager;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.fuzzy.classifier.operator.classification.Classification;
import cilabo.fuzzy.classifier.operator.classification.factory.CFmeanClassification;
import cilabo.gbml.solution.PittsburghSolution;
import cilabo.gbml.solution.util.attribute.multitasking.BirthPlace;
import cilabo.gbml.solution.util.attribute.multitasking.FamilyLine;
import cilabo.gbml.solution.util.attribute.multitasking.ParentOrChild;
import cilabo.main.Consts;
import cilabo.metric.Metric;
import cilabo.metric.RuleLength;
import cilabo.metric.RuleNum;
import cilabo.metric.multilabel.Fmeasure;
import cilabo.metric.multilabel.HammingLoss;
import cilabo.metric.multilabel.Precision;
import cilabo.metric.multilabel.Recall;
import cilabo.metric.multilabel.SubsetAccuracy;
import cilabo.util.ConstantPeriodOutput;
import cilabo.util.OutputFrequency;
import cilabo.utility.Input;
import cilabo.utility.Output;
import cilabo.utility.Parallel;
import cilabo.utility.Random;

public class MultiTasking_Main {


	public static void main(String[] args) throws JMetalException, FileNotFoundException {
		String sep = File.separator;

		/* ********************************************************* */
		System.out.println();
		System.out.println("==== INFORMATION ====");
		String version = "1.0";
		System.out.println("main: " + MultiTasking_Main.class.getCanonicalName());
		System.out.println("version: " + version);
		System.out.println();
		System.out.println("Algorithm: Multi-Tasking Multi-objective Fuzzy Genetics-Based Machine Learning");
		System.out.println("EMOA: NSGA-II");
		System.out.println();
		/* ********************************************************* */
		// Load consts.properties
		Consts.set("consts");
		// make result directory
		Output.mkdirs(Consts.ROOTFOLDER);

		// set command arguments to static variables
		CommandLineArgs.loadArgs(CommandLineArgs.class.getCanonicalName(), args);
		// Output constant parameters
		String fileName = Consts.EXPERIMENT_ID_DIR + sep + "Consts.txt";
		Output.writeln(fileName, Consts.getString(), true);
		Output.writeln(fileName, CommandLineArgs.getParamsString(), true);

		// Initialize ForkJoinPool
		Parallel.getInstance().initLearningForkJoinPool(CommandLineArgs.parallelCores);

		System.out.println("Processors: " + Runtime.getRuntime().availableProcessors() + " ");
		System.out.print("args: ");
		for(int i = 0; i < args.length; i++) {
			System.out.print(args[i] + " ");
		}
		System.out.println();
		System.out.println("=====================");
		System.out.println();

		/* ********************************************************* */
		System.out.println("==== EXPERIMENT =====");
		Date start = new Date();
		System.out.println("START: " + start);

		/* Random Number ======================= */
		Random.getInstance().initRandom(Consts.RAND_SEED);
		JMetalRandom.getInstance().setSeed(Consts.RAND_SEED);

		/* Load Dataset ======================== */
		DataSet train = new DataSet();
		DataSet test = new DataSet();
		Input.inputMultiLabelDataSet(train, CommandLineArgs.trainFile);
		Input.inputMultiLabelDataSet(test, CommandLineArgs.testFile);
		TrainTestDatasetManager datasetManager = new TrainTestDatasetManager();
		datasetManager.addTrains(train);
		datasetManager.addTests(test);

		/* Run MoFGBML algorithm =============== */
		MultiTaskingMoFGBML(train, test);
		/* ===================================== */

		Date end = new Date();
		System.out.println("END: " + end);
		System.out.println("=====================");
		/* ********************************************************* */

		System.exit(0);
	}

	/**
	 *
	 * @param train
	 * @param test
	 */
	public static void MultiTaskingMoFGBML(DataSet train, DataSet test) {
//		String sep = File.separator;

		/* Output frequency: Constant period of evaluations */
		OutputFrequency outputFrequency = new ConstantPeriodOutput(Consts.outputFrequency);

		/* Termination: Number of total evaluations */
		Termination termination = new TerminationByEvaluations(Consts.terminateEvaluation);

		/* Classification: CF-mean Method */
		Classification classification = new CFmeanClassification();

		AbstractEvolutionaryAlgorithm<IntegerSolution, List<IntegerSolution>> algorithm
		= new EveryGenerationSharingTaskManager<>(train,
												  CommandLineArgs.sharingAmount,
												  Consts.populationSize,
												  Consts.offspringPopulationSize,
												  outputFrequency,
												  Consts.EXPERIMENT_ID_DIR,
												  termination,
												  classification);

		/* === ALGORITHM RUN === */
		algorithm.run();
		/* ============== */

		finalPopulationOutput((TaskManager)algorithm, train, test);

		return;
	}

	public static void finalPopulationOutput(TaskManager algorithm,
											 DataSet train,
											 DataSet test)
	{
		/* *********************************** */
		String sep = File.separator;

		List<Task<? extends Solution<?>>> taskList = algorithm.getTaskList();
		int numberOfTasks = taskList.size();

		for(int t = 0; t < numberOfTasks; t++) {
			Task<? extends Solution<?>> task = taskList.get(t);
			@SuppressWarnings("unchecked")
			List<IntegerSolution> solutionList = (List<IntegerSolution>) task.getPopulation();
			int numberOfObjectives = solutionList.get(0).getNumberOfObjectives();

			String outputRootDir = task.getOutputRootDir();

			ArrayList<String> strs = new ArrayList<>();
			String str = "";

			/* Headers */
			//id
			str = "id";
			//fitness
			for(int o = 0; o < numberOfObjectives; o++) {
				str += "," + "f" + String.valueOf(o);
			}
			//NSGA-II
			str += "," + "rank";
			str += "," + "crowding";
			//Multi-Tasking
			str += "," + "current";
			for(int i = 0; i < numberOfTasks; i++) {
				str += "," + "blood_task"+String.valueOf(i+1);
			}
			//BirthPlace
			str += "," + "birthPlace";
			/* Performance metrics */
			str += "," + "ruleNum";
			str += "," + "ruleLength";

			str += "," + "SubsetAccuracy_train";
			str += "," + "HammingLoss_train";
			str += "," + "Fmeasure_train";
			str += "," + "Recall_train";
			str += "," + "Precision_train";

			str += "," + "SubsetAccuracy_test";
			str += "," + "HammingLoss_test";
			str += "," + "Fmeasure_test";
			str += "," + "Recall_test";
			str += "," + "Precision_test";
			strs.add(str);

			/* Body */
			List<IntegerSolution> nondominated = SolutionListUtils.getNonDominatedSolutions(solutionList);
			Metric subAcc = new SubsetAccuracy();
			Metric hammingLoss = new HammingLoss();
			Metric fMeasure = new Fmeasure();
			Metric recall = new Recall();
			Metric precision = new Precision();
			Metric ruleNum = new RuleNum();
			Metric ruleLength = new RuleLength();

			for(int p = 0; p < nondominated.size(); p++) {
				str = "";

				PittsburghSolution solution = (PittsburghSolution) nondominated.get(p);
				RuleBasedClassifier classifier = (RuleBasedClassifier)solution.getClassifier();

				//id
				str = String.valueOf(p);
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
				for(int i = 0; i < numberOfTasks; i++) {
					double value = familyLine.get("task"+(i+1));
					str += "," + value;
				}
				// birth place
				str += "," + solution.getAttribute((new BirthPlace<>()).getAttributeId());

				/* Performance metrics */
				str += "," + ruleNum.metric(classifier);
				str += "," + ruleLength.metric(classifier);

				str += "," + subAcc.metric(classifier, train);
				str += "," + hammingLoss.metric(classifier, train);
				str += "," + fMeasure.metric(classifier, train);
				str += "," + recall.metric(classifier, train);
				str += "," + precision.metric(classifier, train);

				str += "," + subAcc.metric(classifier, test);
				str += "," + hammingLoss.metric(classifier, test);
				str += "," + fMeasure.metric(classifier, test);
				str += "," + recall.metric(classifier, test);
				str += "," + precision.metric(classifier, test);

				strs.add(str);
			}

			String fileName = outputRootDir + sep + "FinalPopulation"+".csv";
			Output.writeln(fileName, strs, false);
		}









		/* *********************************** */
	}
}



























