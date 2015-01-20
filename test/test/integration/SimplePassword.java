package test.integration;

public class SimplePassword {
	
	private static boolean selfDestruct = false;
	
	public static void main(String[] args) throws Exception {
		int password = 52;
		if (passwordClean(password)) {
			if (passwordBig(password)) {
				selfDestruct = true;
			}
		}
		if (selfDestruct)
			throw new Exception();
	}
	
	public static boolean passwordClean(int password) {
		return (password < 50);
	}
	
	public static boolean passwordBig(int password) {
		return (password > 51);
	}
}
