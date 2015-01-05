package constraints;

import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;
import accrue.pdg.node.PDGNodeType;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class ExpressionConstraints {
	public static String getVarName(String name) {
		int eqIdx = name.indexOf("=");
		if (eqIdx != -1) 
			return name.substring(0,eqIdx);
		int locIdx = name.indexOf("LOC ");
		if (locIdx != -1) {
			assert (locIdx == 0);
			String afterLoc = name.substring(4);
			int spaceIdx = afterLoc.indexOf(" ");
			assert (spaceIdx != -1);
			return afterLoc.substring(0, spaceIdx);
		}
		return name;
	}
	
	public static BoolExpr getConst(String name, Context ctx) throws Z3Exception {
		if (name.equals("1")) return ctx.MkTrue();
		if (name.equals("0")) return ctx.MkFalse();
		return null;
	}

	// searches for node with name "name" amongst the parents of this node in the z3var map
	public static Expr getZ3VarFromSources(String name, AbstractPDGNode node, ProgramDependenceGraph pdg, 
									Map<Integer,Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {

		Set<PDGEdge> edges = pdg.incomingEdgesOf(node); 
		for (PDGEdge edge : edges) {
			AbstractPDGNode source = edge.getSource();
			String sourceName = getVarName(source.getName());
			if (name.equals(sourceName)) {
				Expr added =  IntraProcedureConstraints.getOrAddAnyVar(expNodeToZ3Var, source.getNodeId(), ctx);
				return added;
			}
		}
		// this should never happen
		return Z3Addons.getFreshBoolVar(ctx);
	}
	
	public static BoolExpr getPhiExp(String name, AbstractPDGNode node, ProgramDependenceGraph pdg,
			Map<Integer, Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Expr nodeVar = IntraProcedureConstraints.getOrAddAnyVar(expNodeToZ3Var, node.getNodeId(), ctx);
		if (name.indexOf("phi") != -1) {
			int start = name.indexOf("(");
			int endfirst = name.indexOf(",");
			int end = name.indexOf(")");
			String leftName = name.substring(start + 1, endfirst);
			String rightName = name.substring(endfirst + 1, end);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			return ctx.MkOr(new BoolExpr[] {ctx.MkEq(nodeVar, leftVar), ctx.MkEq(nodeVar, rightVar)});
		}
		return null;
	}
	
	public static BoolExpr getUnaryExp(String name, AbstractPDGNode node, ProgramDependenceGraph pdg,
								Map<Integer, Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Expr nodeVar = IntraProcedureConstraints.getOrAddAnyVar(expNodeToZ3Var, node.getNodeId(), ctx);
		Expr rhs = getZ3VarFromSources(name, node, pdg, expNodeToZ3Var, ctx);
		return ctx.MkEq(nodeVar, rhs);
	}
							
	public static BoolExpr getExpConstraint(AbstractPDGNode node, ProgramDependenceGraph pdg, 
											Map<Integer, Expr> expNodeToZ3Var, Context ctx) 
															throws Z3Exception {
		Expr nodeVar = IntraProcedureConstraints.getOrAddAnyVar(expNodeToZ3Var, node.getNodeId(), ctx);
		String name = node.getName();
		
		if (node.getNodeType() == PDGNodeType.BASE_VALUE) {
			return ctx.MkEq(nodeVar, getConst(name, ctx));
		}
		
		if (name.indexOf("=") != -1) {
			name = name.substring(name.indexOf("=") + 2);
		}
		else {
			// < case
			// > case
			// ! case
			// phi case
			BoolExpr phiExp = getPhiExp(name, node, pdg, expNodeToZ3Var, ctx);
			if (phiExp != null) return phiExp;
		}
		// unary case
		return getUnaryExp(name, node, pdg, expNodeToZ3Var, ctx);
	}
}
