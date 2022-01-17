package cilabo.gbml.objectivefunction.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.solution.integersolution.IntegerSolution;

import cilabo.data.ClassLabel;
import cilabo.data.DataSet;
import cilabo.data.Pattern;
import cilabo.data.RejectedClassLabel;
import cilabo.fuzzy.classifier.RuleBasedClassifier;
import cilabo.fuzzy.rule.RejectedRule;
import cilabo.fuzzy.rule.Rule;
import cilabo.gbml.objectivefunction.ObjectiveFunction;
import cilabo.gbml.solution.MichiganSolution;
import cilabo.gbml.solution.PittsburghSolution;
import cilabo.gbml.solution.util.attribute.ErroredPatternsAttribute;
import cilabo.gbml.solution.util.attribute.NumberOfWinner;

/**
 * Pittsburgh個体から，マルチラベルデータセットのそれぞれのパターンに対する予測ラベルを計算する．
 * 各種識別性能評価尺度に関しては，別途計算してもらう．
 * このとき，誤識別パターンをAttributeとしてPittsburghSolutionインスタンスに保持させる．
 *
 */
public class MultiLabelOutputOfPittsburgh implements ObjectiveFunction<PittsburghSolution, ArrayList<ClassLabel>> {
	// ************************************
	private DataSet data;

	// ************************************
	public MultiLabelOutputOfPittsburgh(DataSet data) {
		this.data = data;
	}

	// ************************************

	/**
	 * ピッツバーグ個体を受け取って，
	 *  + michiganPopulationの評価（勝利回数・NCP）
	 *  + 誤識別パターンのAttribute付与
	 *  + 各パターンに対する予測ラベル計算
	 * を行うメソッド.
	 * @param solution PittsburghSolution
	 * @return {@literal ArrayList<ClassLabel>} :
	 */
	@Override
	public ArrayList<ClassLabel> function(PittsburghSolution solution) {
		List<IntegerSolution> michiganPopulation = solution.getMichiganPopulation();
		/* Clear fitness of michigan population. */
		michiganPopulation.stream().forEach(s -> s.setObjective(0, 0.0));
		/* Clear Attribute of NumberOfWinner for michigan population. */
		michiganPopulation.stream().forEach(s -> s.setAttribute((new NumberOfWinner<>()).getAttributeId(), 0));
		/* Clear Attribute of ErroredPatternsAttribute for pittsburgh population. */
		solution.setAttribute((new ErroredPatternsAttribute<>()).getAttributeId(), new ArrayList<Integer>());

		// for Evaluation without Duplicates
		Map<String, IntegerSolution> map = new HashMap<>();
		for(int i = 0; i < michiganPopulation.size(); i++) {
			MichiganSolution michiganSolution = (MichiganSolution)michiganPopulation.get(i);
			Rule rule = michiganSolution.getRule();
			if(!map.containsKey(rule.toString())) {
				map.put(rule.toString(), michiganSolution);
			}
		}

		// Classification
		ArrayList<ClassLabel> classifiedLabels = new ArrayList<>();
		RuleBasedClassifier classifier = (RuleBasedClassifier)solution.getClassifier();
		for(int i = 0; i < data.getDataSize(); i++) {
			Pattern pattern = data.getPattern(i);
			Rule winnerRule = classifier.classify(pattern.getInputVector());

			// If output is rejected then continue next pattern.
			if(winnerRule.getClass() == RejectedRule.class) {
				/* Add errored pattern Attribute */
				addErroredPattern(solution, pattern.getID());
				classifiedLabels.add(RejectedClassLabel.getInstance());
				continue;
			}

			if(winnerRule != null) {
				/* Add Attribute of NumberOfWinner */
				String attributeId = (new NumberOfWinner<>()).getAttributeId();
				Integer Nwin = (Integer)map.get(winnerRule.toString()).getAttribute(attributeId);
				map.get(winnerRule.toString()).setAttribute(attributeId, Nwin+1);

				/* Rule fitness */
				ClassLabel classifiedLabel = winnerRule.getConsequent().getClassLabel();
				ClassLabel trueClassLabel = pattern.getTrueClass();
				double match = 0.0;
				int Lnum = classifiedLabel.getClassVector().length;
				for(int l = 0; l < Lnum; l++) {
					if(trueClassLabel.getClassVector()[l] == classifiedLabel.getClassVector()[l]) {
						match += 1.0;
					}
				}

				double fitness = match / (double)Lnum;

				/*TODO 半分以上間違えていれば誤識別パターンとして保持するように変更？？ */
//				if(fitness <= (double)Lnum/2.0) {
//					addErroredPattern(solution, pattern.getID());
//				}

				IntegerSolution winnerMichigan = map.get(winnerRule.toString());
				fitness += winnerMichigan.getObjective(0);
				winnerMichigan.setObjective(0, fitness);

				/* Errored patterns  */
				if(!classifiedLabel.toString().equals(trueClassLabel.toString())) {
					addErroredPattern(solution, pattern.getID());
				}

				classifiedLabels.add(winnerRule.getConsequent().getClassLabel());
			}
		}

		return classifiedLabels;
	}

	@SuppressWarnings("unchecked")
	private void addErroredPattern(PittsburghSolution solution, int patternID) {
		ArrayList<Integer> erroredList = (ArrayList<Integer>)solution.getAttribute((new ErroredPatternsAttribute<>()).getAttributeId());
		erroredList.add(patternID);
		return;
	}
}
