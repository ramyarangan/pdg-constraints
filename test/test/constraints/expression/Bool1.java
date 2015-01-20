package test.constraints.expression;

public class Bool1 {
    @SuppressWarnings("unused")
	private static boolean z;
	
    public static void main(String[] args) {
        boolean y = false;
        boolean x = true && y;
        if (x) {
            z = false;
        }
    }
}
