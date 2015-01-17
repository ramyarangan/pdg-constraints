package test.constraints.interprocedural;

public class MultipleCallDisjunction {
	private static boolean y;
    @SuppressWarnings("unused")
	private static boolean x;
	
	public static void main(String[] args) {
		y = false;
		if (y) { 
			y = foo();
		}
		if (foo())
			y = true;
		if (y)
			x = false;
	}
	
	public static boolean foo() {
		return true;
	}
}
