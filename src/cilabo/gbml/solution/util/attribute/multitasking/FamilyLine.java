package cilabo.gbml.solution.util.attribute.multitasking;

import java.util.Comparator;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.util.attribute.Attribute;

public class FamilyLine<S extends Solution<?>> implements Attribute<S> {

	private String attributeId = getClass().getName();
	private Comparator<S> solutionComparator;

	public FamilyLine() {
	}

	@Override
	public String getAttributeId() {
		return attributeId;
	}

	@Override
	public Comparator<S> getSolutionComparator() {
		return solutionComparator;
	}

}
