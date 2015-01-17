package test.constraints.interprocedural;

public class MultipleCallImprecision3 {
    @SuppressWarnings("unused")
    private static boolean y;
    
	public static void main(String[] args) {
		if (foo(true))
			if (foo(false))
				y = true;
	}
	
	public static boolean foo(boolean x) {
		return x;
	}

}
