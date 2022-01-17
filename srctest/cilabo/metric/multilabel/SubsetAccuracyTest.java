package cilabo.metric.multilabel;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.data.InputVector;
import cilabo.data.Pattern;
import cilabo.data.RejectedClassLabel;
import cilabo.fuzzy.StaticFuzzyClassifierForTest;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.metric.Metric;
import cilabo.utility.Input;

public class SubsetAccuracyTest {
	@Test
	public void testMetric1() {
		String sep = File.separator;
		String dataName = "dataset" + sep + "richromatic" + sep + "a0_0_richromatic-10tra.dat";
		DataSet train = new DataSet();
		Input.inputMultiLabelDataSet(train, dataName);

		RuleBasedClassifier classifier = StaticFuzzyClassifierForTest.makeMultiLabelClassifier(train);

		Metric errorRate = new SubsetAccuracy();

		double expected = (double)errorRate.metric(classifier, train);
		double diff = 0.006;
		assertEquals(expected, 88.51851851851852, diff);
	}

	@Test
	public void testMetric2() {
		DataSet dataset = new DataSet();

		int id = 0;
		double[] vector = new double[] {0, 1};
		InputVector inputVector = new InputVector(vector);
		ClassLabel classLabel = new ClassLabel();
		classLabel.addClassLabels(new Integer[] {1, 1, 1});

		Pattern pattern = Pattern.builder()
							.id(id)
							.inputVector(inputVector)
							.trueClass(classLabel)
							.build();
		dataset.addPattern(pattern);
		dataset.setDataSize(1);

		ClassLabel reject = RejectedClassLabel.getInstance();
		ArrayList<ClassLabel> classifiedClassLabels = new ArrayList<>();
		classifiedClassLabels.add(reject);


		Metric metric = new SubsetAccuracy();
		double subAcc = (double)metric.metric(classifiedClassLabels, dataset);
		double diff = 0.006;
		assertEquals(subAcc, 0.0, diff);
	}


}
