package test.constraints.loop;

public class WhileLoopSeenNow {
	@SuppressWarnings("unused")
	private static boolean x;
	
	public static void main(String[] args) {
		int i = 3;
		while (i < 4) {
			i += 2;
		}
		if (i < 6) {
			x = true;
		}
	}
}
