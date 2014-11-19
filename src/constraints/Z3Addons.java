package constraints;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Z3Exception;

public class Z3Addons {
	private static int freshVarCount = 0;

	public static BoolExpr getFreshBoolVar(Context ctx) throws Z3Exception {
        Sort bool_type = ctx.BoolSort();

		String freshVar = Integer.toString(freshVarCount);
		freshVarCount++;
		return (BoolExpr) ctx.MkConst(freshVar,bool_type);
	}
	
	public static BoolExpr getFreshIntVar(Context ctx) throws Z3Exception {
        Sort int_type = ctx.IntSort();

		String freshVar = Integer.toString(freshVarCount);
		freshVarCount++;
		return (BoolExpr) ctx.MkConst(freshVar,int_type);
	}
}
