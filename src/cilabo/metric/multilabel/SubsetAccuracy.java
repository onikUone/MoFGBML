package cilabo.metric.multilabel;

import java.util.ArrayList;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.data.InputVector;
import cilabo.data.RejectedClassLabel;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.metric.Metric;

public class SubsetAccuracy implements Metric {
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
		double size = dataset.getDataSize();
		double correct = 0;
		for(int p = 0; p < size; p++) {
			InputVector vector = dataset.getPattern(p).getInputVector();
			ClassLabel trueClass = dataset.getPattern(p).getTrueClass();

			ClassLabel classifiedClass = classifier.classify(vector).getConsequent().getClassLabel();

			if( trueClass.toString().equals( classifiedClass.toString() )) {
				correct += 1;
			}
		}

		return 100.0 * correct/size;
	}

	public Double metric(ArrayList<ClassLabel> classifiedLabels, DataSet dataset) {
		double size = dataset.getDataSize();
		if(size != classifiedLabels.size()) {
			return null;
		}

		double correct = 0;
		for(int p = 0; p < size; p++) {
			ClassLabel trueClass = dataset.getPattern(p).getTrueClass();
			ClassLabel classifiedClass = classifiedLabels.get(p);

			if(classifiedClass.getClass() == RejectedClassLabel.class) {
				continue;
			}

			if( trueClass.toString().equals(classifiedClass.toString()) ) {
				correct += 1;
			}
		}

		return 100.0 * correct/size;
	}


}
