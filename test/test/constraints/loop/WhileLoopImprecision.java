package test.constraints.loop;

public class WhileLoopImprecision {
	@SuppressWarnings("unused")
	private static boolean x;
	
	public static void main(String[] args) {
		int i = 3;
		while (i < 5) {
			i += 3;
		}
		if (i < 6) {
			x = true;
		}
	}
}
