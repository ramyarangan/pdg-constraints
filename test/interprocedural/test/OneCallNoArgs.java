package interprocedural.test;

public class OneCallNoArgs {
    @SuppressWarnings("unused")
    private static boolean y;

    public static void main(String[] args) {
     if (foo())
    	 y = true;
    }
    
    public static boolean foo() {
    	return false;
    }

}
