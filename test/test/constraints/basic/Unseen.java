package test.constraints.basic;

public class Unseen {
    static boolean y;
    static boolean x;

    public static void main(String[] args) {

        x = false;
        if (y) {
            if (!y) {
                x = true;
            }
        }
    }
}
