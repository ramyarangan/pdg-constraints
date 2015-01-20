package constraints;
import java.util.Map;
import java.util.Set;

import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class InterProcedure {
	public static void getFunctionConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
												Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var,
												Map<Integer, Expr> expNodeToZ3Var,
												Set<BoolExpr> constraints) 
													throws Z3Exception {
		// control flow constraints
		BoolExpr nodePCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		AbstractPDGNode calleeNode = null;
		if (PDGHelper.isReturnNode(node, pdg))
			calleeNode = PDGHelper.getCrossFunctionNode(node, pdg, true);
		else if (PDGHelper.isCallerNode(node, pdg))
			calleeNode = PDGHelper.getCrossFunctionNode(node, pdg, false);
		BoolExpr calleeNodePCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, calleeNode.getNodeId(), ctx);
		constraints.add(ctx.MkEq(nodePCVar, calleeNodePCVar));
		
		// expression constraints
		if (!PDGHelper.isExprNode(node)) return;
		
		BoolExpr target = null;
		if (PDGHelper.isReturnNode(node, pdg))
			target = nodePCVar;
		else if (PDGHelper.isCallerNode(node, pdg))
			target = calleeNodePCVar;
		Expr nodeExprVar = Expression.getOrAddAnyVar(expNodeToZ3Var, node, ctx);
		Expr calleeExprVar = Expression.getOrAddAnyVar(expNodeToZ3Var, calleeNode, ctx);

		constraints.add(ctx.MkImplies(target, ctx.MkEq(nodeExprVar, calleeExprVar)));
	}

	public static BoolExpr getUniqueFuncConstraints(AbstractPDGNode node, 
										BoolExpr origFuncConstraint, 
										BoolExpr funcConstraint,
										BoolExpr origEqExp, 
										Context ctx) throws Z3Exception {
		if (origEqExp != null && PDGHelper.isExprNode(node)) {
			origFuncConstraint = Z3Addons.removeConstraintContainingExp(origFuncConstraint, origEqExp, ctx);
		}
		if (origEqExp != null && !PDGHelper.isExprNode(node)) {
			BoolExpr removedSubConstraint = 
					Z3Addons.removeConstraintContainingExp(origFuncConstraint, origEqExp, ctx);
			if (removedSubConstraint == null) {
				if (origFuncConstraint != null) return origFuncConstraint;
			} else {
				if (!removedSubConstraint.equals(origFuncConstraint)) 
					return origFuncConstraint;
			}
		}
		return Z3Addons.orConstraints(origFuncConstraint, ctx, funcConstraint);
	}
	
	public static void updateFuncConstraint(AbstractPDGNode node, 
											ProgramDependenceGraph pdg,
											Set<BoolExpr> constraints,
											Map<String, BoolExpr> funcToConstraint,
											Context ctx,
											Map<Integer, BoolExpr> pdgNodeToZ3Var) 
													throws Z3Exception {
		// construct constraint conjunction
		BoolExpr nodePCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		BoolExpr origEqExp = null;
		BoolExpr funcConstraint = null;
		for (BoolExpr constraint : constraints) {
			funcConstraint = Z3Addons.andConstraints(funcConstraint, ctx, constraint);
			
			// capture an equality constraint that don't include the given node, if
			// the given node is a return variable - this equality constraint is in
			// another possibility for funcToConstraint iff the constraints for this
			// function were already built, but without the return variable and its
			// constraint contributions
			if (constraint.IsEq() && 
					!Z3Addons.containsVar(constraint, nodePCVar)) {
				origEqExp = constraint;
			}
		}
		
		// find function name for this call
		String functionName = PDGHelper.getFunctionNameForCall(node, pdg);
		
		// later in the iteration, we may have run into a formal assignment when a 
		// constraint was originally based on the exit PC - this is a more specific 
		// constraint then, so we add it here.
		BoolExpr origFuncConstraint = funcToConstraint.get(functionName);
		funcConstraint = getUniqueFuncConstraints(node, origFuncConstraint, funcConstraint, origEqExp, ctx);
		funcToConstraint.put(functionName, funcConstraint);
	}

	public static void getEntryNodeConstraints(Set<AbstractPDGNode> nodes, 
											ProgramDependenceGraph pdg, 
											Context ctx, 
											Map<Integer, BoolExpr> pdgNodeToZ3Var, 
											Map<Integer, Expr> expNodeToZ3Var, 
											Set<BoolExpr> constraints) 
											throws Z3Exception {
		assert(!nodes.isEmpty());
		
		// obtain all call site labels
		Set<Integer> labels = PDGHelper.getAllSiteLabels(nodes, pdg);
		
		// assemble constraints
		BoolExpr fullConstraint = null;
		for (int labelId : labels) {
			BoolExpr constraintPerLabel = null;
			for (AbstractPDGNode node : nodes) {
				AbstractPDGNode sourceLabel = PDGHelper.getLabelPredecessor(node, pdg, labelId);
				int nodeId = node.getNodeId();
				int sourceId = sourceLabel.getNodeId();
				// PC constraint
				BoolExpr nodePCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, nodeId, ctx);
				BoolExpr sourceLabelPCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, sourceId, ctx);
				BoolExpr pcConstraint = ctx.MkEq(nodePCVar, sourceLabelPCVar);
				constraintPerLabel = Z3Addons.andConstraints(constraintPerLabel, ctx, pcConstraint);
				
				// Exp constraint
				if (PDGHelper.isExprNode(node)) {
					System.out.println(node.getName() + " " + node.getJavaType());
					System.out.println(sourceLabel.getName() + " " + sourceLabel.getJavaType());
					Expr nodeExpVar = Expression.getOrAddAnyVar(expNodeToZ3Var, node, ctx);
					Expr sourceLabelExpVar = Expression.getOrAddAnyVar(expNodeToZ3Var, sourceLabel, ctx);
					BoolExpr expConstraint = ctx.MkImplies(nodePCVar, 
												ctx.MkEq(nodeExpVar, sourceLabelExpVar));
					constraintPerLabel = Z3Addons.andConstraints(constraintPerLabel, ctx, expConstraint);	
				}
			}
			fullConstraint = Z3Addons.orConstraints(fullConstraint, ctx, constraintPerLabel);
		}
		
		constraints.add(fullConstraint);
	}
}
