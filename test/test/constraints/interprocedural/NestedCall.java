package test.constraints.interprocedural;

public class NestedCall {
	public static void main(String[] args) {
		foo(true);
		bar(false);
	}
	
	public static boolean foo(boolean x) {
		return x;
	}
	
	public static boolean bar(boolean x) {
		return foo(x);
	}
}