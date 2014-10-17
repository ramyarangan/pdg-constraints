
import com.microsoft.z3.*;


public class ConstraintCheck {
    Model Check(Context ctx, boolean useMBQI, BoolExpr[] assertions) 
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
        Log.Append("SimpleExample");

        {
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
                    .println("prove: store(a1, i1, v1) = store(a2, i2, v2) implies (i1 = i3 or i2 = i3 or select(a1, i3) = select(a2, i3))");
            System.out.println(thm);
            Prove(ctx, thm, false);
            /* be kind to dispose manually and not wait for the GC. */
            ctx.Dispose();
        }
    }
	
	public static void main(String[] args) throws Z3Exception {
        ConstraintCheck c = new ConstraintCheck();
        c.SimpleExample();
	}

}
