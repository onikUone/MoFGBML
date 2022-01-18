package cilabo.metric.multilabel;

import java.util.ArrayList;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.data.InputVector;
import cilabo.data.RejectedClassLabel;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.metric.Metric;

public class Fmeasure implements Metric {
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

		double recall = 0.0;
		double precision = 0.0;
		for(int p = 0; p < size; p++) {
			InputVector vector = dataset.getPattern(p).getInputVector();
			Integer[] trueClass = dataset.getPattern(p).getTrueClass().getClassVector();

			ClassLabel classifiedLabel = classifier.classify(vector)
					.getConsequent().getClassLabel();
			if(classifiedLabel.getClass() == RejectedClassLabel.class) {
				continue;
			}
			Integer[] classifiedClass = classifiedLabel.getClassVector();

			precision += Precision.PrecisionMetric(classifiedClass, trueClass);
			recall += Recall.RecallMetric(classifiedClass, trueClass);
		}
		recall = recall/size;
		precision = precision/size;

		double Fmeasure;
		if((precision + recall) == 0) Fmeasure = 0;
		else {
			Fmeasure = (2.0 * recall * precision) / (recall + precision);
		}
		return 100.0 * Fmeasure;
	}

	public Double metric(ArrayList<ClassLabel> classifiedLabels, DataSet dataset) {
		double size = dataset.getDataSize();
		if(size != classifiedLabels.size()) {
			return null;
		}

		double recall = 0.0;
		double precision = 0.0;
		for(int p = 0; p < size; p++) {
			Integer[] trueClass = dataset.getPattern(p).getTrueClass().getClassVector();
			if(classifiedLabels.get(p).getClass() == RejectedClassLabel.class) {
				continue;
			}
			Integer[] classifiedClass = classifiedLabels.get(p).getClassVector();

			precision += Precision.PrecisionMetric(classifiedClass, trueClass);
			recall += Recall.RecallMetric(classifiedClass, trueClass);
		}
		recall = recall/size;
		precision = precision/size;

		double Fmeasure;
		if((precision + recall) == 0) Fmeasure = 0;
		else {
			Fmeasure = (2.0 * recall * precision) / (recall + precision);
		}
		return 100.0 * Fmeasure;
	}

}
