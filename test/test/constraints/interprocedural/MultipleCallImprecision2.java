package test.constraints.interprocedural;

public class MultipleCallImprecision2 {
    @SuppressWarnings("unused")
    private static boolean y;
    
	public static void main(String[] args) {
		if (foo(false))
			y = true;
		if (foo(false))
			y = true;
	}
	
	public static boolean foo(boolean x) {
		return x;
	}

}
