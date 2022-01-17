package cilabo.labo.developing.multitasking;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

import org.uma.jmetal.algorithm.impl.AbstractEvolutionaryAlgorithm;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import cilabo.data.DataSet;
import cilabo.data.impl.TrainTestDatasetManager;
import cilabo.fuzzy.classifier.operator.classification.Classification;
import cilabo.fuzzy.classifier.operator.classification.factory.CFmeanClassification;
import cilabo.main.Consts;
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

//		List<Task<? extends Solution<?>>> taskList = ((TaskManager)algorithm).getTaskList();


	}
}



























