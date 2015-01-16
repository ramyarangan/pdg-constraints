package test.constraints.basic;

public class UnseenAnd {
    static boolean y;
    static boolean x;
    static boolean z;

    public static void main(String[] args) {

        x = false;
        if (y && z) {
            if (!y && z) {
                x = true;
            }
        }
    }
}