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
}
