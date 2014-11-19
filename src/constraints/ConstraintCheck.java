package constraints;

import java.util.Set;

import com.microsoft.z3.*;


public class ConstraintCheck {
	
	public static Model Check(Context ctx, Set<BoolExpr> assertions) throws Z3Exception {
		return Check(ctx, assertions, false);
	}
	
    public static Model Check(Context ctx, Set<BoolExpr> assertions, boolean useMBQI) 
    		throws Z3Exception {
		Solver s = ctx.MkSolver();
        Params p = ctx.MkParams();
        p.Add("mbqi", useMBQI);
        s.setParameters(p);
        
        for (BoolExpr a : assertions)
        	s.Assert(a);
		if (s.Check() == Status.SATISFIABLE)
		    return s.Model();
		else
		    return null;
	}
    
    public void SimpleExample() throws Z3Exception
    {
        System.out.println("SimpleExample");

        Context ctx = new Context();
            
        /* do something with the context */
        Sort int_type = ctx.IntSort();
        
        Expr a = ctx.MkConst("a", int_type);
        ArithExpr asquared = ctx.MkMul(new ArithExpr[] {(ArithExpr) a,(ArithExpr) a});
        BoolExpr eq1 = ctx.MkGt(asquared, ctx.MkInt(2));
        BoolExpr eq2 = ctx.MkLe((ArithExpr) a, ctx.MkInt(5));
        BoolExpr eq3 = ctx.MkGt((ArithExpr) a, ctx.MkInt(0));
        
        BoolExpr[] assertions = {eq1,eq2,eq3};
        
        System.out.println("Checking 0 < a <= 5, a^2 > 3");
        Model model = Check(ctx, assertions);
        System.out.println(model);
    }
    
	public void ArrayExample() throws Z3Exception
    {
        System.out.println("SimpleExample");

        Context ctx = new Context();
            
        /* do something with the context */
        Sort int_type = ctx.IntSort();
        Sort array_type = ctx.MkArraySort(int_type, int_type);

        ArrayExpr a1 = (ArrayExpr) ctx.MkConst("a1", array_type);
        ArrayExpr a2 = ctx.MkArrayConst("a2", int_type, int_type);
        Expr i1 = ctx.MkConst("i1", int_type);
        Expr i2 = ctx.MkConst("i2", int_type);
        Expr i3 = ctx.MkConst("i3", int_type);
        Expr v1 = ctx.MkConst("v1", int_type);
        Expr v2 = ctx.MkConst("v2", int_type);

        Expr st1 = ctx.MkStore(a1, i1, v1);
        Expr st2 = ctx.MkStore(a2, i2, v2);

        Expr sel1 = ctx.MkSelect(a1, i3);
        Expr sel2 = ctx.MkSelect(a2, i3);

        /* create antecedent */
        BoolExpr antecedent = ctx.MkEq(st1, st2);

        /*
         * create consequent: i1 = i3 or i2 = i3 or select(a1, i3) = select(a2,
         * i3)
         */
        BoolExpr consequent = ctx.MkOr(new BoolExpr[] { ctx.MkEq(i1, i3),
                ctx.MkEq(i2, i3), ctx.MkEq(sel1, sel2) });

        /*
         * prove store(a1, i1, v1) = store(a2, i2, v2) implies (i1 = i3 or i2 =
         * i3 or select(a1, i3) = select(a2, i3))
         */
        BoolExpr thm = ctx.MkImplies(antecedent, consequent);
        System.out
                .println("findsat: store(a1, i1, v1) = store(a2, i2, v2) implies (i1 = i3 or i2 = i3 or select(a1, i3) = select(a2, i3))");
        System.out.println(thm);
        BoolExpr[] assertions = {thm};
        Model model = Check(ctx, assertions);
        System.out.println(model);
    }
	
	public static void main(String[] args) throws Z3Exception {
        ConstraintCheck c = new ConstraintCheck();
        c.SimpleExample();
	}
}
