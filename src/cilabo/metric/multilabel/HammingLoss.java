package cilabo.metric.multilabel;

import java.util.ArrayList;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.data.InputVector;
import cilabo.data.RejectedClassLabel;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.metric.Metric;
import cilabo.utility.GeneralFunctions;

public class HammingLoss implements Metric {
	// ************************************************************
	// Fields

	// ************************************************************
	// Constructor

	// ************************************************************
	// Methods

	/**
	 * @param classifier : FuzzyClassifier
	 * @param dataset : DataSet
	 * @return Double
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Double metric(Object... objects) {
		RuleBasedClassifier classifier = null;
		DataSet dataset = null;
		ArrayList<ClassLabel> classifiedLabels = null;
		for(Object object : objects) {
			if(object.getClass() == RuleBasedClassifier.class) {
				classifier = (RuleBasedClassifier)object;
			}
			else if(object.getClass() == DataSet.class) {
				dataset = (DataSet)object;
			}
			else if(object.getClass() == ArrayList.class) {
				classifiedLabels = (ArrayList<ClassLabel>)object;
			}
			else {
				(new IllegalArgumentException()).printStackTrace();
				return null;
			}
		}

		if(classifier != null && dataset != null) {
			return metric(classifier, dataset);
		}
		else if(classifiedLabels != null && dataset != null) {
			return metric(classifiedLabels, dataset);
		}
		else {
			return null;
		}
	}

	public Double metric(RuleBasedClassifier classifier, DataSet dataset) {
		double size = dataset.getDataSize();	// Number of instances;
		double noClass = dataset.getCnum();		// Number of classes;

		double HammingLoss = 0;
		for(int p = 0; p < size; p++) {
			InputVector vector = dataset.getPattern(p).getInputVector();
			Integer[] trueClass = dataset.getPattern(p).getTrueClass().getClassVector();

			Integer[] classifiedClass = classifier.classify(vector)
													.getConsequent().getClassLabel()
													.getClassVector();

			double distance = GeneralFunctions.HammingDistance(trueClass, classifiedClass);
			HammingLoss += distance / noClass;
		}

		return 100.0 * HammingLoss/size;
	}

	public Double metric(ArrayList<ClassLabel> classifiedLabels, DataSet dataset) {
		double size = dataset.getDataSize();	// Number of instances;
		double noClass = dataset.getCnum();		// Number of classes;

		if(size != classifiedLabels.size()) {
			return null;
		}

		double hammingLoss = 0.0;
		for(int p = 0; p < size; p++) {
			Integer[] trueClass = dataset.getPattern(p).getTrueClass().getClassVector();
			if(classifiedLabels.get(p).getClass() == RejectedClassLabel.class) {
				continue;
			}
			Integer[] classifiedClass = classifiedLabels.get(p).getClassVector();

			double distance = GeneralFunctions.HammingDistance(trueClass, classifiedClass);
			hammingLoss += distance / noClass;
		}

		return 100.0 * hammingLoss/size;
	}

}
