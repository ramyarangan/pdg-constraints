package constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;
import accrue.pdg.node.PDGNodeType;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class Expression {
	public static Expr getOrAddAnyVar(Map<Integer, Expr> mapToZ3Var, AbstractPDGNode node, Context ctx) 
			throws Z3Exception {
		if (mapToZ3Var.containsKey(node.getNodeId())) {
			return mapToZ3Var.get(node.getNodeId());
		}
		
		String expName = getExpressionStr(node.getName());
		if (expName.equals("1") || expName.equals("0") || 
				expName.equals("phi(1, 0)") ||
				expName.equals("phi(0, 1)") ||
				expName.contains("^")) {
			Expr nodeVar = Z3Addons.getFreshBoolVar(ctx);
			mapToZ3Var.put(node.getNodeId(), nodeVar);
			return nodeVar;
		}
		
		Expr nodeVar;
		switch (node.getJavaType()) {
			case "I":
				nodeVar = Z3Addons.getFreshIntVar(ctx);
				break;
			case "Z":
				nodeVar = Z3Addons.getFreshBoolVar(ctx);
				break;
			default:
				throw new IllegalArgumentException("Node type unexpected: " + node.getJavaType());
		}
		mapToZ3Var.put(node.getNodeId(), nodeVar);
		
		return nodeVar;
	}
	
	public static Expr getBaseVar(AbstractPDGNode node, Context ctx) throws Z3Exception {
		String name = node.getName();
		if (name.equals("1")) return ctx.MkTrue();
		if (name.equals("0")) return ctx.MkFalse();
		
		switch (node.getJavaType()) {
			case "I":
				return ctx.MkInt(Integer.valueOf(name));
			default:
				throw new IllegalArgumentException("Node type unexpected: " + node.getJavaType());
		}
	}
	
	public static BoolExpr getBaseValConstraint(AbstractPDGNode node, Expr nodeVar, 
												ProgramDependenceGraph pdg, Context ctx) 
																throws Z3Exception {
		if ((node.getNodeType() == PDGNodeType.BASE_VALUE) && pdg.incomingEdgesOf(node).isEmpty()) {
			return ctx.MkEq(nodeVar, getBaseVar(node, ctx));
		}
		return null;
	}
	
	public static String getExpressionStr(String name) {
		if (name.contains("=") && 
				!(name.contains(">=") || name.contains("<="))) {
			if (name.contains("formal-0")) {
				name = name.substring(0, name.indexOf("for ") - 1);
			}
			name = name.substring(name.indexOf("=") + 2);
		}
		
		if (name.contains("return")) {
			name = name.substring(name.indexOf("return") + 7);
		}
		
		return name;
	}
	
	public static String getParentNodeName(String name) {
		int eqIdx = name.indexOf("=");
		int binopIdx = name.indexOf(">=");
		if (binopIdx == -1) binopIdx = name.indexOf("<=");
		if ((eqIdx != -1) &&
				(binopIdx == -1)) 
			return name.substring(0,eqIdx-1);
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
	
	// searches for node with name "name" amongst the parents of this node in the z3var map
	public static Expr getZ3VarFromSources(String name, AbstractPDGNode node, ProgramDependenceGraph pdg, 
									Map<Integer,Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node); 
		for (PDGEdge edge : edges) {
			AbstractPDGNode source = edge.getSource();
			String sourceName = getParentNodeName(source.getName());
			if (name.equals(sourceName)) {
				return getOrAddAnyVar(expNodeToZ3Var, source, ctx);
			}
		}

		return null;
	}
	
	// searches for node with name "name" amongst the parents of this node in the z3var map
	public static Integer getZ3IdFromSources(String name, AbstractPDGNode node, ProgramDependenceGraph pdg, 
									Map<Integer,Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node); 
		for (PDGEdge edge : edges) {
			AbstractPDGNode source = edge.getSource();
			String sourceName = getParentNodeName(source.getName());
			if (name.equals(sourceName)) {
				return source.getNodeId();
			}
		}

		return null;
	}

	
	public static BoolExpr getPhiExp(String name, AbstractPDGNode node, ProgramDependenceGraph pdg,
			Map<Integer, Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Expr nodeVar = getOrAddAnyVar(expNodeToZ3Var, node, ctx);
		if (!name.contains("phi"))
			return null;
		int start = name.indexOf("(");
		int endfirst = name.indexOf(",");
		int end = name.indexOf(")");
		String leftName = name.substring(start + 1, endfirst);
		String rightName = name.substring(endfirst + 2, end);
		Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
		Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
		if ((leftVar != null) && (rightVar != null))
			return ctx.MkOr(new BoolExpr[] {ctx.MkEq(nodeVar, leftVar), ctx.MkEq(nodeVar, rightVar)});
		if (leftVar == null) leftVar = rightVar;
		if (leftVar != null) return ctx.MkEq(nodeVar,  leftVar);
		return null;
	}
	
	public static BoolExpr getBinopExp(String name, AbstractPDGNode node, ProgramDependenceGraph pdg,
			Map<Integer, Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Expr nodeVar = getOrAddAnyVar(expNodeToZ3Var, node, ctx);
		Expr exp = null;
		if (name.contains("^")) {
			int opIndex = name.indexOf("^");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 2);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			exp = ctx.MkXor((BoolExpr)leftVar, (BoolExpr)rightVar);
		}
		else if (name.contains(">=")) {
			int opIndex = name.indexOf(">=");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 3);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			
			exp = ctx.MkGe((ArithExpr)leftVar, (ArithExpr)rightVar);		
		}
		else if (name.contains("<=")) {
			int opIndex = name.indexOf("<=");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 3);
			System.out.println(leftName + " " + rightName);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			
			exp = ctx.MkLe((ArithExpr)leftVar, (ArithExpr)rightVar);		
		}
		else if (name.contains(">")) {
			int opIndex = name.indexOf(">");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 2);
			System.out.println(leftName + " " + rightName);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			
			exp = ctx.MkGt((ArithExpr)leftVar, (ArithExpr)rightVar);		
		}
		else if (name.contains("<")) {
			int opIndex = name.indexOf("<");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 2);
			System.out.println(leftName + " " + rightName);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			
			exp = ctx.MkLt((ArithExpr)leftVar, (ArithExpr)rightVar);		
		}
		else if (name.contains("+")) {
			int opIndex = name.indexOf("+");
			String leftName = name.substring(0, opIndex - 1);
			String rightName = name.substring(opIndex + 2);
			System.out.println(leftName + " " + rightName);
			Expr leftVar = getZ3VarFromSources(leftName, node, pdg, expNodeToZ3Var, ctx);
			Expr rightVar = getZ3VarFromSources(rightName, node, pdg, expNodeToZ3Var, ctx);
			
			exp = ctx.MkAdd(new ArithExpr[] {(ArithExpr)leftVar, (ArithExpr)rightVar});		
		}
		if (exp == null)
			return null;
		return ctx.MkEq(nodeVar, exp);
	}
	
	/**
	 * multiple values can equal a return node value, say. 
	 * 
	 * @param node
	 * @param pdg
	 * @param expNodeToZ3Var
	 * @param ctx
	 * @return
	 * @throws Z3Exception
	 */
	public static List<Expr> getExpSourceNodes(AbstractPDGNode node, 
										ProgramDependenceGraph pdg, 
										Map<Integer, Expr> expNodeToZ3Var, 
										Context ctx) throws Z3Exception {
		List<Expr> expSourceNodes = new ArrayList<Expr>();
		
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			AbstractPDGNode source = edge.getSource();
			if (PDGHelper.isExprNode(source)) {
				Expr newVar = getOrAddAnyVar(expNodeToZ3Var, source, ctx);
				expSourceNodes.add(newVar);
			}
		}
		return expSourceNodes;
	}
	
	public static BoolExpr getUnaryExp(String name, AbstractPDGNode node, ProgramDependenceGraph pdg,
								Map<Integer, Expr> expNodeToZ3Var, Context ctx) throws Z3Exception {
		Expr nodeVar = getOrAddAnyVar(expNodeToZ3Var, node, ctx);
		List<Expr> subExps = new ArrayList<Expr>();
		
		subExps = getExpSourceNodes(node, pdg, expNodeToZ3Var, ctx);
		
		BoolExpr retExp = null;
		for (Expr subExp : subExps) {
			retExp = Z3Addons.orConstraints(retExp, ctx, ctx.MkEq(nodeVar,  subExp));
		}
		return retExp;
	}

	public static BoolExpr getZ3ExpressionEq(String name, AbstractPDGNode node, 
											ProgramDependenceGraph pdg, 
											Map<Integer, Expr> expNodeToZ3Var, Context ctx)
		
													throws Z3Exception {
		// phi case
		BoolExpr phiExp = getPhiExp(name, node, pdg, expNodeToZ3Var, ctx);
		if (phiExp != null) return phiExp;
		
		// < > + etc cases
		BoolExpr binopExp = getBinopExp(name, node, pdg, expNodeToZ3Var, ctx);
		if (binopExp != null) return binopExp;
		
		// unary case
		return getUnaryExp(name, node, pdg, expNodeToZ3Var, ctx);
	}

	public static BoolExpr getExpConstraint(AbstractPDGNode node, ProgramDependenceGraph pdg, 
											Map<Integer, Expr> expNodeToZ3Var, Context ctx) 
															throws Z3Exception {		
		Expr nodeVar = getOrAddAnyVar(expNodeToZ3Var, node, ctx);
		String name = node.getName();
		
		BoolExpr baseValConstraint = getBaseValConstraint(node, nodeVar, pdg, ctx);
		if (baseValConstraint != null) return baseValConstraint;
		
		name = getExpressionStr(name);
		return getZ3ExpressionEq(name, node, pdg, expNodeToZ3Var, ctx);
	}
}
