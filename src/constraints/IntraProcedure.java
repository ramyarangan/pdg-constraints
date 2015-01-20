package constraints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.PDGEdgeType;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class IntraProcedure {

	public static BoolExpr getMergeControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
					Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {
		BoolExpr pcConstraint = null;
	
		AbstractPDGNode mergeNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.MERGE);
		if (mergeNode != null) {
			for (PDGEdge edge : edges) {
				assert(edge.getType() == PDGEdgeType.MERGE);
				mergeNode = edge.getSource();
				BoolExpr newConstraint = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, mergeNode.getNodeId(), ctx);
				pcConstraint = Z3Addons.orConstraints(pcConstraint, ctx, newConstraint);
			}
		}
		
		return pcConstraint;
	}

	public static BoolExpr getBooleanControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
											Context ctx, 
											Map<Integer, BoolExpr> pdgNodeToZ3Var, 
											Map<Integer, Expr> expNodeToZ3Var) throws Z3Exception {
		AbstractPDGNode booleanNode = null;
		BoolExpr booleanNodeExp = null;
		
		// expression boolean constraint
		// true type
		AbstractPDGNode trueNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.TRUE);
		if (trueNode != null) {
			booleanNode = trueNode;
			System.out.println(booleanNode.getName() + " " + booleanNode.getJavaType());
			booleanNodeExp = (BoolExpr) Expression.getOrAddAnyVar(expNodeToZ3Var, trueNode, ctx);
		}
		
		// false type
		AbstractPDGNode falseNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.FALSE);
		if (falseNode != null) {
			booleanNode = falseNode;
			booleanNodeExp = (BoolExpr) Expression.getOrAddAnyVar(expNodeToZ3Var, falseNode, ctx);
			booleanNodeExp = ctx.MkNot(booleanNodeExp);
		}	
		
		// PC boolean constraint
		if (booleanNode != null) {
			BoolExpr booleanNodePCVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, booleanNode.getNodeId(), ctx);
			return ctx.MkAnd(new BoolExpr[]{booleanNodeExp, booleanNodePCVar});
		}
		return null;
	}

	public static BoolExpr getCopyExplicitControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode copyExplicitNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.COPY);
		if (copyExplicitNode == null) {
			copyExplicitNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.EXP);
		}
		if (copyExplicitNode != null) {
			for (PDGEdge edge : edges) {
				if ((edge.getType() == PDGEdgeType.COPY) || (edge.getType() == PDGEdgeType.EXP)) {
					copyExplicitNode = edge.getSource();
					BoolExpr newConstraint = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, copyExplicitNode.getNodeId(), ctx);
					pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx, newConstraint);
				}
			}
		}
		
		return pcConstraint;
	}

	public static BoolExpr getImplicitControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		AbstractPDGNode implicitNode = PDGHelper.getSourceNodeByType(edges,PDGEdgeType.IMPLICIT);
		if (implicitNode != null) {
			return PDGConstraint.getOrAddVar(pdgNodeToZ3Var, implicitNode.getNodeId(), ctx);
		}
		return null;
	}

	public static BoolExpr getConjunctionControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode conjunctionNode = PDGHelper.getSourceNodeByType(edges, PDGEdgeType.CONJUNCTION);
		if (conjunctionNode != null) {
			for (PDGEdge edge : edges) {
				AbstractPDGNode source = edge.getSource();
				if (edge.getType() == PDGEdgeType.CONJUNCTION) {
					BoolExpr newVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, source.getNodeId(), ctx);
					pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx, newVar);
				}
			}
		}
		
		return pcConstraint;
	}

	public static BoolExpr addIntraProceduralPCConstraints(BoolExpr pcConstraint, Context context, 
											Set<PDGEdge> edges,
											ProgramDependenceGraph pdg,
											Context ctx,
											Map<Integer, BoolExpr> pdgNodeToZ3Var,
											Map<Integer, Expr> expNodeToZ3Var) 
											throws Z3Exception {
		// merge type
		pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx,
				getMergeControlFlowConstraints(edges, pdg, ctx, pdgNodeToZ3Var));
		
		// true type
		pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx,
				getBooleanControlFlowConstraints(edges, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var));
	
		// copy and explicit type
		pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx,
				getCopyExplicitControlFlowConstraints(edges, pdg, ctx, pdgNodeToZ3Var));
		
		// implicit type
		pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx,
				getImplicitControlFlowConstraints(edges, pdg, ctx, pdgNodeToZ3Var));
	
		// conjunction type
		pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx,
				getConjunctionControlFlowConstraints(edges, pdg, ctx, pdgNodeToZ3Var));
		
		return pcConstraint;
	}

	/**
	 * Note: Constraints based on labeled Entry and Exit edges are NOT returned here.
	 * 
	 * @param node
	 * @param pdg
	 * @param ctx
	 * @param pdgNodeToZ3Var
	 * @return
	 * @throws Z3Exception
	 */
	public static void getControlFlowConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg, 
													Context ctx, 
													Map<Integer, BoolExpr> pdgNodeToZ3Var,
													Map<Integer, Expr> expNodeToZ3Var,
													Set<BoolExpr> constraints) 
													throws Z3Exception {				
		Set<PDGEdge> graphEdges = pdg.incomingEdgesOf(node);
		Set<PDGEdge> edges = new HashSet<PDGEdge>(graphEdges);
				
		// remove all edges that are entry or exit edges; these are handled separately
		// in function constraint formation
		List<PDGEdge> removeList = new ArrayList<PDGEdge>();
		for (PDGEdge edge : edges) {
			if (edge.getEdgeLabel() != null) removeList.add(edge);
		}
		edges.removeAll(removeList);
		if (edges.isEmpty()) return;
	
		BoolExpr pcConstraint = null;
		BoolExpr nodeVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
	
		pcConstraint = addIntraProceduralPCConstraints(pcConstraint, ctx, 
									edges, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var);
	
		if (pcConstraint != null)
			constraints.add(ctx.MkEq(nodeVar, pcConstraint));
	}

	public static void addMergeConstraint(BoolExpr expConstraint, AbstractPDGNode node, 
			ProgramDependenceGraph pdg, Context ctx, 
			Map<Integer, BoolExpr> pdgNodeToZ3Var, 
			Map<Integer, Expr> expNodeToZ3Var, 
			Set<BoolExpr> constraints) throws Z3Exception {
		Expr[] args = expConstraint.Args();
		AbstractPDGNode parent = null;
		for (PDGEdge incoming : pdg.incomingEdgesOf(node)) {
			Expr expVar = expNodeToZ3Var.get(incoming.getSource().getNodeId());
			if (expVar.equals(args[0]) || expVar.equals(args[1])) {
				parent = incoming.getSource();
				break;
			}
		}
		AbstractPDGNode pcParent = PDGHelper.getSourceNodeByType(pdg.incomingEdgesOf(parent), 
																PDGEdgeType.IMPLICIT);
		BoolExpr pcParentVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, pcParent.getNodeId(), ctx);
		BoolExpr pcNodeVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		BoolExpr parentVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, parent.getNodeId(), ctx);
		constraints.add(ctx.MkEq(ctx.MkAnd(new BoolExpr[]{expConstraint, pcNodeVar}), 
														pcParentVar));
		constraints.add(ctx.MkImplies(expConstraint, parentVar));
	}

	public static void addMergeConstraints(BoolExpr expConstraint, AbstractPDGNode node, 
							ProgramDependenceGraph pdg, Context ctx, 
							Map<Integer, BoolExpr> pdgNodeToZ3Var, 
							Map<Integer, Expr> expNodeToZ3Var, 
							Set<BoolExpr> constraints) throws Z3Exception {
		while (expConstraint.IsOr()) {
			addMergeConstraint((BoolExpr) expConstraint.Args()[1], node, pdg, ctx, 
										pdgNodeToZ3Var, expNodeToZ3Var, constraints);
			expConstraint = (BoolExpr) expConstraint.Args()[0];
		}
		addMergeConstraint(expConstraint, node, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
	}

	public static void getExpressionConstraints(AbstractPDGNode node, 
									ProgramDependenceGraph pdg, Context ctx, 
									Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var, 
									Set<BoolExpr> constraints) throws Z3Exception {
		// skip nodes that receive arguments in a called function or receive return values
		// in the caller function. These are function constraints which will be handled later.
		if (PDGHelper.isEntryNode(node, pdg) || PDGHelper.isReturnNode(node, pdg))
			return;
	
		BoolExpr expConstraint = null;
		
		if (PDGHelper.isExprNode(node)) {
			// expression constraint should be something like this val constraint = 
			// some combination of parent's val constraints.
			BoolExpr nodeConstraint = Expression.getExpConstraint(node, pdg, expNodeToZ3Var, ctx);
			if (nodeConstraint != null) {
				expConstraint = nodeConstraint;
				if (PDGConstraint.debugMode) System.out.println("Node constraint for " + node.getName() + " " + expConstraint);
			}
		}
		
		BoolExpr nodeVar = PDGConstraint.getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		if (expConstraint != null) {
			constraints.add(ctx.MkImplies(nodeVar, expConstraint));
			if (PDGHelper.isPhiOrMergeNode(node, pdg)) addMergeConstraints(expConstraint, node, pdg, ctx, pdgNodeToZ3Var, 
											expNodeToZ3Var, constraints);
		}
	}

	public static void getNonFunctionConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
												Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var,
												Map<Integer, Expr> expNodeToZ3Var,
												Set<BoolExpr> constraints) 
												throws Z3Exception {
		getControlFlowConstraints(node, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
		getExpressionConstraints(node, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);	
	}

}
