package test.constraints.expression;

public class Int1 {
    @SuppressWarnings("unused")
	private static int y;
	
    public static void main(String[] args) {
        int y = 5;
        if (y < 4) {
        	y = 6; 
        }
    }
}

