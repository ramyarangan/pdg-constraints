package test.constraints.loop;

public class WhileLoopUnseen {
	@SuppressWarnings("unused")
	private static boolean x;
	
	public static void main(String[] args) {
		int i = 3;
		while (i < 5) {
			i += 2;
		}
		if (i < 5) {
			x = true;
		}
	}
}
