package test.constraints.interprocedural;

public class OneCallOneArg {
    @SuppressWarnings("unused")
	private static boolean y;
	
	public static void main(String[] args) {
		if (foo(false))
			y = false;
	}
	
	public static boolean foo(boolean x) {
		return x;
	}
}
