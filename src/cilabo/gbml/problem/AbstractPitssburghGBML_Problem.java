package cilabo.gbml.problem;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.Solution;

import cilabo.fuzzy.classifier.operator.classification.Classification;
import cilabo.fuzzy.knowledge.Knowledge;
import cilabo.fuzzy.rule.antecedent.AntecedentFactory;
import cilabo.fuzzy.rule.consequent.ConsequentFactory;

public abstract class AbstractPitssburghGBML_Problem<S extends Solution<?>> extends AbstractIntegerProblem {
	// ************************************************************
	/**  */
	protected AntecedentFactory antecedentFactory;
	/**  */
	protected ConsequentFactory consequentFactory;

	/** */
	protected Knowledge knowledge;

	/** */
	protected Classification classification;

	// ************************************************************


	// ************************************************************
	/* Getters */
	public AntecedentFactory getAntecedentFactory() {
		return this.antecedentFactory;
	}
	public ConsequentFactory getConsequentFactory() {
		return this.consequentFactory;
	}

	public Knowledge getKnowledge() {
		return this.knowledge;
	}

	public Classification getClassification() {
		return this.classification;
	}

	/* Setters */
	public void setAntecedentFactory(AntecedentFactory antecedentFactory) {
		this.antecedentFactory = antecedentFactory;
	}
	public void setConsequentFactory(ConsequentFactory consequentFactory) {
		this.consequentFactory = consequentFactory;
	}

	public void setKnowledge(Knowledge knowledge) {
		this.knowledge = knowledge;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}


	@Override
	public String toString() {
		String ln = System.lineSeparator();
		String str = "";
		str += "AntecedentFactory: " + antecedentFactory.getClass().getCanonicalName() + ln;
		str += "ConsequentFactory: " + consequentFactory.getClass().getCanonicalName() + ln;
		str += "Classification: " + classification.getClass().getCanonicalName() + ln;
		return str;
	}
}
