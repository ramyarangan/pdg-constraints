package test.constraints.basic;

public class BoolTest1 {
    private static boolean x;
    private static boolean y;
    @SuppressWarnings("unused")
    private static boolean z;

    public static void main(String[] args) {
        z = true;
        y = false;
        x = true && y;
        if (x) {
            z = false;
        }
    }
}
