package test.constraints.basic;

public class BoolTest2 {
    public static void main(String[] args) {
        foo(true);
        foo(false);
    }

    public static boolean foo(boolean x) {
        boolean y;
        boolean z;
        if (x) {
            y = true;
        }
        else {
            y = false;
        }
        if (x) {
            z = false;
        }
        else {
            z = true;
        }

        if (!(y ^ z)) {
            return false;
        }
        return true;
    }
}
