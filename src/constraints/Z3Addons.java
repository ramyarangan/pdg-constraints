package constraints;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
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
	
	
	public static BoolExpr andConstraints(BoolExpr oldConstraint, Context ctx, BoolExpr andConstraint) 
			throws Z3Exception {
		if (oldConstraint != null) {
			if (andConstraint != null) {
				return ctx.MkAnd(new BoolExpr[] {oldConstraint, andConstraint});
			}
			return oldConstraint;
		}
		return andConstraint;
	}
	
	public static BoolExpr orConstraints(BoolExpr oldConstraint, Context ctx, BoolExpr orConstraint) 
			throws Z3Exception {
		if (oldConstraint != null) {
			if (orConstraint != null) {
				return ctx.MkOr(new BoolExpr[] {oldConstraint, orConstraint});
			}
			return oldConstraint;
		}
		return orConstraint;
	}

	public static boolean containsVar(BoolExpr constraint, Expr var) throws Z3Exception {
		for (Expr arg : constraint.Args()) {
			if (arg.equals(var)) return true;
		}
		return false;
	}
	
	public static boolean equalityConstraintsEqual(BoolExpr eqConstraint1, BoolExpr eqConstraint2) 
														throws Z3Exception {
		assert (eqConstraint1.IsEq() && eqConstraint2.IsEq());
		
		Expr[] firstArgs = eqConstraint1.Args();
		Expr[] secondArgs = eqConstraint2.Args();
		
		if ((firstArgs[0].equals(secondArgs[0]) &&
			firstArgs[1].equals(secondArgs[1])) || 
				(firstArgs[0].equals(secondArgs[1]) &&
					firstArgs[1].equals(secondArgs[0])))
			return true;
		return false;
	}
	
	public static boolean containsEqExp(BoolExpr andConstraint, BoolExpr exp) 
																throws Z3Exception {
		if (andConstraint == null) return false;
		
		BoolExpr curConstraint = andConstraint;
		while (curConstraint.IsAnd()) {
			BoolExpr compExpr = (BoolExpr) curConstraint.Args()[1];
			curConstraint = (BoolExpr) curConstraint.Args()[0];
			
			if (compExpr.IsEq() &&
					equalityConstraintsEqual((BoolExpr) compExpr, exp)) {
				return true;
			}
		}
		if (curConstraint.IsEq() &&
				equalityConstraintsEqual((BoolExpr) curConstraint, exp)) {
			return true;
		}
		return false;
	}
	
	public static BoolExpr removeConstraintContainingExp(BoolExpr orConstraint, 
												BoolExpr exp, Context ctx) 
														throws Z3Exception {
		if (orConstraint == null) return null;
		
		ArrayList<BoolExpr> allConstraints = new ArrayList<BoolExpr>();
		while (orConstraint.IsOr()) {
			Expr[] orConstraintArgs = orConstraint.Args();
			orConstraint = (BoolExpr) orConstraintArgs[0];
			BoolExpr andConstraint = (BoolExpr) orConstraintArgs[1];
			if (!containsEqExp(andConstraint, exp))
				allConstraints.add((BoolExpr) orConstraintArgs[1]);
		}
		if (!containsEqExp(orConstraint, exp))
			allConstraints.add(orConstraint);
		
		BoolExpr newConstraint = null;
		for (BoolExpr constraint : allConstraints) {
			newConstraint = orConstraints(newConstraint, ctx, constraint);
		}

		return newConstraint;
	}
}
