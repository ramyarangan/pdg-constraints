package test.constraints.interprocedural;

public class MultipleCallImprecision1 {

    @SuppressWarnings("unused")
    private static boolean y;
    
	public static void main(String[] args) {
		if (foo(true))
			y = true;
		if (foo(false))
			y = true;
	}
	
	public static boolean foo(boolean x) {
		return x;
	}

}
