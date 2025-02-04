package cilabo.fuzzy.rule.antecedent;

import java.util.Arrays;

import cilabo.fuzzy.knowledge.Knowledge;
import jfml.term.FuzzyTermType;

public class Antecedent {
	// ************************************************************
	// Fields
	int[] antecedentIndex;

	FuzzyTermType[] antecedentFuzzySets;

	// ************************************************************
	// Constructor

	/**
	 * private constructor for deepcopy method
	 * @param antecedentIndex
	 * @param antecedentFuzzySets
	 */
	private Antecedent(int[] antecedentIndex, FuzzyTermType[] antecedentFuzzySets) {
		this.antecedentIndex = antecedentIndex;
		this.antecedentFuzzySets = antecedentFuzzySets;
	}

	/**
	 *
	 * @param knowledge
	 * @param antecedentIndex : Shallow copy
	 */
	public Antecedent(Knowledge knowledge, int[] antecedentIndex) {
		this.antecedentIndex = antecedentIndex;
		this.antecedentFuzzySets = new FuzzyTermType[antecedentIndex.length];
		for(int i = 0; i < antecedentIndex.length; i++) {
			if(antecedentIndex[i] < 0) {
				// Categorical
				antecedentFuzzySets[i] = null;
			}
			else {
				// Numerical
				antecedentFuzzySets[i] = knowledge.getFuzzySet(i, antecedentIndex[i]);
			}
		}
	}

	// ************************************************************
	// Methods

	/**
	 *
	 */
	public Antecedent deepcopy() {
		int[] antecedentIndex = Arrays.copyOf(this.antecedentIndex, this.antecedentIndex.length);
		return new Antecedent(antecedentIndex, this.antecedentFuzzySets);
	}

	/**
	 *
	 */
	public double getCompatibleGrade(double[] x) {
		double grade = 1;
		for(int i = 0; i < x.length; i++) {
			if(antecedentIndex[i] < 0) {
				// categorical
				if(antecedentIndex[i] == (int)x[i]) grade *= 1.0;
				else grade *= 0.0;
			}
			else {
				// numerical
				grade *= antecedentFuzzySets[i].getMembershipValue((float)x[i]);
			}
		}
		return grade;
	}

	/**
	 *
	 */
	public int getDimension() {
		return this.antecedentIndex.length;
	}

	/**
	 *
	 */
	public int getAntecedentIndexAt(int dimension) {
		return this.antecedentIndex[dimension];
	}

	/**
	 *
	 */
	public int[] getAntecedentIndex() {
		return this.antecedentIndex;
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	public FuzzyTermType getAntecedentFuzzySetAt(int index) {
		return this.antecedentFuzzySets[index];
	}

	/**
	 *
	 */
	public int getRuleLength() {
		int length = 0;
		for(int i = 0; i < antecedentIndex.length; i++) {
			if(antecedentIndex[i] != 0) {
				length++;
			}
		}
		return length;
	}

	@Override
	public String toString() {
		if(antecedentFuzzySets == null) return null;

		String str = antecedentFuzzySets[0].getName();
		for(int i = 1; i < antecedentFuzzySets.length; i++) {
			str += ", " + antecedentFuzzySets[i].getName();
		}
		return str;
	}


	public static AntecedentBuilder builder() {
		return new AntecedentBuilder();
	}

	public static class AntecedentBuilder {
		private Knowledge knowledge;
		private int[] antecedentIndex;

		AntecedentBuilder() {}

		public Antecedent.AntecedentBuilder knowledge(Knowledge knowledge) {
			this.knowledge = knowledge;
			return this;
		}

		public Antecedent.AntecedentBuilder antecedentIndex(int[] antecedentIndex) {
			this.antecedentIndex = antecedentIndex;
			return this;
		}

		/**
		 * @param knowledge : Knowledge
		 * @param antecedentIndex : int[]
		 */
		public Antecedent build() {
			return new Antecedent(knowledge, antecedentIndex);
		}
	}

}
