package cilabo.data;

public class RejectedClassLabel extends ClassLabel {
	private static RejectedClassLabel instance;

	public static RejectedClassLabel getInstance() {
		if(instance == null) {
			instance = new RejectedClassLabel();
		}
		return instance;
	}

	@Override
	public String toString() {
		return "rejected";
	}

}
