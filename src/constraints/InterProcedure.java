package constraints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.PDGEdgeType;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;
import accrue.pdg.node.AbstractPDGNode;
import accrue.pdg.node.PDGNodeType;
import accrue.pdg.util.CallSiteEdgeLabel;
import accrue.pdg.util.CallSiteEdgeLabel.SiteType;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Z3Exception;

public class InterProcedure {
	private static final boolean debugMode = true;
		
	public static BoolExpr getOrAddVar(Map<Integer, BoolExpr> mapToZ3Var, int id, Context ctx) 
				throws Z3Exception {
		BoolExpr nodeVar = null;
		if (mapToZ3Var.containsKey(id)) {
			nodeVar = mapToZ3Var.get(id);
		}
		else {
			nodeVar = Z3Addons.getFreshBoolVar(ctx);
			mapToZ3Var.put(id, nodeVar);
		}
		return nodeVar;
	}
	
	// the following methods should probably be in the node and edge classes	
	public static boolean isExprNode(AbstractPDGNode node) {
		switch (node.getNodeType()) {
			case LOCAL:
			case BASE_VALUE:
			case OTHER_EXPRESSION:
			case EXIT_ASSIGNMENT:
			case EXIT_SUMMARY:
			case FORMAL_ASSIGNMENT:
			case FORMAL_SUMMARY:
			case ABSTRACT_LOCATION:
				return true;
			default:
				return false;
		}
	}

	public static boolean isPCNode(AbstractPDGNode node) {
		return !isExprNode(node);
	}
	
	public static boolean isPhiOrMergeNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		return ((node.getName().indexOf("phi") == 0) ||
				(getSourceNodeByType(pdg.incomingEdgesOf(node), PDGEdgeType.MERGE) != null));
	}
	
	public static boolean isReturnNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		Set<PDGEdge> incomingEdges = pdg.incomingEdgesOf(node);
		for (PDGEdge edge : incomingEdges) {
			CallSiteEdgeLabel label = edge.getEdgeLabel();
			if ((label != null) && (label.getType() == SiteType.EXIT)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCallerNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		Set<PDGEdge> outgoingEdges = pdg.outgoingEdgesOf(node);
		for (PDGEdge edge : outgoingEdges) {
			CallSiteEdgeLabel label = edge.getEdgeLabel();
			if ((label != null) && (label.getType() == SiteType.ENTRY)) return true;
		}
		return false;				
	}
	
	public static boolean isExitNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		Set<PDGEdge> outgoingEdges = pdg.outgoingEdgesOf(node);
		for (PDGEdge edge : outgoingEdges) {
			CallSiteEdgeLabel label = edge.getEdgeLabel();
			if ((label != null) && (label.getType() == SiteType.EXIT)) return true;
		}
		return false;
	}

	
	public static boolean isEntryNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		Set<PDGEdge> incomingEdges = pdg.incomingEdgesOf(node);
		for (PDGEdge edge : incomingEdges) {
			CallSiteEdgeLabel label = edge.getEdgeLabel();
			if ((label != null) && (label.getType() == SiteType.ENTRY)) return true;
		}
		return false;		
	}
	
	/**
	 * TODO: When this is used, it's for debug purposes. The final version of this will not 
	 * stop at the main method. 
	 * 
	 * @param node
	 * @param pdg
	 * @return
	 */
	public static boolean isMainEntry(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		if (isEntryNode(node, pdg) &&
				(node.getProcedureName().indexOf("main") != -1)) {
			return true;
		}
		return false;		
	}	
	
	public static AbstractPDGNode getLabelPredecessor(AbstractPDGNode node, 
													ProgramDependenceGraph pdg, 
													int labelID) {
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			if ((edge.getEdgeLabel() != null) &&
				(edge.getEdgeLabel().getCallSiteID() == labelID))
				return edge.getSource();
		}
		return null;
	}
	
	public static Set<Integer> getAllSiteLabels(Set<AbstractPDGNode> nodes, ProgramDependenceGraph pdg) {
		if (nodes.size() == 0) return null;
		
		Set<Integer> labels = new HashSet<Integer>();
		Iterator<AbstractPDGNode> nodeIterator = nodes.iterator();
		AbstractPDGNode anEntryNode = nodeIterator.next();
		
		for (PDGEdge edge : pdg.incomingEdgesOf(anEntryNode)) {
			assert (edge.getEdgeLabel() != null);
			labels.add(edge.getEdgeLabel().getCallSiteID());
		}
		return labels;
	}
	
	public static AbstractPDGNode getCrossFunctionNode(AbstractPDGNode node, 
									ProgramDependenceGraph pdg, boolean searchIncoming) {
		String functionName = node.getProcedureName();
		List<AbstractPDGNode> neighbors = new ArrayList<AbstractPDGNode>();
		
		if (searchIncoming) {
			Set<PDGEdge> edges = pdg.incomingEdgesOf(node);
			for (PDGEdge edge : edges) {
				neighbors.add(edge.getSource());
			}
		} else {
			Set<PDGEdge> edges = pdg.outgoingEdgesOf(node);
			for (PDGEdge edge : edges) {
				neighbors.add(edge.getTarget());
			}
		}
		
		for (AbstractPDGNode neighbor : neighbors) {
			if (!neighbor.getProcedureName().equals(functionName)) return neighbor;
		}
		return null;
	}
	
	public static String getFunctionNameForCall(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		AbstractPDGNode calleeNode = null;
		if (isReturnNode(node, pdg))
			calleeNode = getCrossFunctionNode(node, pdg, true);
		else if (isCallerNode(node, pdg))
			calleeNode = getCrossFunctionNode(node, pdg, false);
		return calleeNode.getProcedureName();
	}
	
	public static AbstractPDGNode getSourceNodeByType(Set<PDGEdge> edges, PDGEdgeType type) {
		for (PDGEdge edge : edges) {
			if (edge.getType() == type)
				return edge.getSource();
		}
		return null;
	}
	
	/**
	 * Returns list of PDG nodes representing the backwards slice of
	 * the nodes involved in the function call if the current node is a node in the
	 * caller function representing the return of the called function
	 * 
	 * @param node
	 * @param pdg
	 * @return
	 */
	public static Set<AbstractPDGNode> getFunctionCallNodes(AbstractPDGNode node, 
									ProgramDependenceGraph pdg) {
		if (!isReturnNode(node, pdg)) return null;
		
		Set<AbstractPDGNode> nodes = new HashSet<AbstractPDGNode>();
		String functionName = node.getProcedureName(); 
				
		// add return node and find PC node in caller
		if (node.getNodeType() == PDGNodeType.EXIT_ASSIGNMENT) {
			nodes.add(node);
			for (PDGEdge incoming : pdg.incomingEdgesOf(node)) {
				AbstractPDGNode sourceNode = incoming.getSource();
				if (isReturnNode(sourceNode, pdg) &&
					functionName.equals(sourceNode.getProcedureName())) {
					node = sourceNode;
					break;
				}
			}
		}
		
		// add PC node from caller representing function return
		nodes.add(node);
		
		// add PC node from caller representing function call
		for (PDGEdge incoming : pdg.incomingEdgesOf(node)) {
			AbstractPDGNode sourceNode = incoming.getSource();
			if (isCallerNode(sourceNode, pdg) &&
					functionName.equals(sourceNode.getProcedureName())) {
				node = sourceNode;
				break;
			}
		}
		nodes.add(node);
		
		// add formal argument assignment nodes in caller
		for (PDGEdge outgoing : pdg.outgoingEdgesOf(node)) {
			AbstractPDGNode nextNode = outgoing.getTarget();
			if (isCallerNode(nextNode, pdg) &&
					functionName.equals(nextNode.getProcedureName()))
				nodes.add(nextNode);
		}
		
		return nodes;
	}
	
	public static AbstractPDGNode getCallerPCNode(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		if (!isEntryNode(node, pdg)) return null;
		if (node.getNodeType() == PDGNodeType.ENTRY_PC_SUMMARY) {
			return getCrossFunctionNode(node, pdg, true);
		}
		assert(node.getNodeType() == PDGNodeType.FORMAL_SUMMARY);
		AbstractPDGNode formalCallerNode = getCrossFunctionNode(node, pdg, true);
		return getSourceNodeByType(pdg.incomingEdgesOf(formalCallerNode), PDGEdgeType.IMPLICIT);
	}
	
	public static Set<AbstractPDGNode> getEntryNodes(AbstractPDGNode node, ProgramDependenceGraph pdg) {
		Set<AbstractPDGNode> nodes = new HashSet<AbstractPDGNode>();
		AbstractPDGNode callerPC = getCallerPCNode(node, pdg);
		if (callerPC == null) return null;
		// add entry PC node
		nodes.add(getCrossFunctionNode(callerPC, pdg, false));
		
		// add formal argument node
		for (PDGEdge edge : pdg.outgoingEdgesOf(callerPC)) {
			AbstractPDGNode formalCallerNode = edge.getTarget();
			if (isCallerNode(formalCallerNode, pdg)) {
				nodes.add(getCrossFunctionNode(formalCallerNode, pdg, false));
			}
		}
		
		return nodes;
	}
	

	public static BoolExpr getMergeControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
					Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {
		BoolExpr pcConstraint = null;

		AbstractPDGNode mergeNode = getSourceNodeByType(edges,PDGEdgeType.MERGE);
		if (mergeNode != null) {
			for (PDGEdge edge : edges) {
				assert(edge.getType() == PDGEdgeType.MERGE);
				mergeNode = edge.getSource();
				BoolExpr newConstraint = getOrAddVar(pdgNodeToZ3Var, mergeNode.getNodeId(), ctx);
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
		AbstractPDGNode trueNode = getSourceNodeByType(edges,PDGEdgeType.TRUE);
		if (trueNode != null) {
			booleanNode = trueNode;
			System.out.println(booleanNode.getName() + " " + booleanNode.getJavaType());
			booleanNodeExp = (BoolExpr) Expression.getOrAddAnyVar(expNodeToZ3Var, trueNode, ctx);
		}
		
		// false type
		AbstractPDGNode falseNode = getSourceNodeByType(edges,PDGEdgeType.FALSE);
		if (falseNode != null) {
			booleanNode = falseNode;
			booleanNodeExp = (BoolExpr) Expression.getOrAddAnyVar(expNodeToZ3Var, falseNode, ctx);
			booleanNodeExp = ctx.MkNot(booleanNodeExp);
		}	
		
		// PC boolean constraint
		if (booleanNode != null) {
			BoolExpr booleanNodePCVar = getOrAddVar(pdgNodeToZ3Var, booleanNode.getNodeId(), ctx);
			return ctx.MkAnd(new BoolExpr[]{booleanNodeExp, booleanNodePCVar});
		}
		return null;
	}
	
	public static BoolExpr getCopyExplicitControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode copyExplicitNode = getSourceNodeByType(edges,PDGEdgeType.COPY);
		if (copyExplicitNode == null) {
			copyExplicitNode = getSourceNodeByType(edges,PDGEdgeType.EXP);
		}
		if (copyExplicitNode != null) {
			for (PDGEdge edge : edges) {
				if ((edge.getType() == PDGEdgeType.COPY) || (edge.getType() == PDGEdgeType.EXP)) {
					copyExplicitNode = edge.getSource();
					BoolExpr newConstraint = getOrAddVar(pdgNodeToZ3Var, copyExplicitNode.getNodeId(), ctx);
					pcConstraint = Z3Addons.andConstraints(pcConstraint, ctx, newConstraint);
				}
			}
		}
		
		return pcConstraint;
	}
	
	public static BoolExpr getImplicitControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		AbstractPDGNode implicitNode = getSourceNodeByType(edges,PDGEdgeType.IMPLICIT);
		if (implicitNode != null) {
			return getOrAddVar(pdgNodeToZ3Var, implicitNode.getNodeId(), ctx);
		}
		return null;
	}

	public static BoolExpr getConjunctionControlFlowConstraints(Set<PDGEdge> edges, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode conjunctionNode = getSourceNodeByType(edges, PDGEdgeType.CONJUNCTION);
		if (conjunctionNode != null) {
			for (PDGEdge edge : edges) {
				AbstractPDGNode source = edge.getSource();
				if (edge.getType() == PDGEdgeType.CONJUNCTION) {
					BoolExpr newVar = getOrAddVar(pdgNodeToZ3Var, source.getNodeId(), ctx);
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
		BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);

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
		AbstractPDGNode pcParent = getSourceNodeByType(pdg.incomingEdgesOf(parent), 
																PDGEdgeType.IMPLICIT);
		BoolExpr pcParentVar = getOrAddVar(pdgNodeToZ3Var, pcParent.getNodeId(), ctx);
		BoolExpr pcNodeVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		BoolExpr parentVar = getOrAddVar(pdgNodeToZ3Var, parent.getNodeId(), ctx);
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
		if (isEntryNode(node, pdg) || isReturnNode(node, pdg))
			return;

		BoolExpr expConstraint = null;
		
		if (isExprNode(node)) {
			// expression constraint should be something like this val constraint = 
			// some combination of parent's val constraints.
			BoolExpr nodeConstraint = Expression.getExpConstraint(node, pdg, expNodeToZ3Var, ctx);
			if (nodeConstraint != null) {
				expConstraint = nodeConstraint;
				if (debugMode) System.out.println("Node constraint for " + node.getName() + " " + expConstraint);
			}
		}
		
		BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		if (expConstraint != null) {
			constraints.add(ctx.MkImplies(nodeVar, expConstraint));
			if (isPhiOrMergeNode(node, pdg)) addMergeConstraints(expConstraint, node, pdg, ctx, pdgNodeToZ3Var, 
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
	
	public static void getFunctionConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
												Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var,
												Map<Integer, Expr> expNodeToZ3Var,
												Set<BoolExpr> constraints) 
													throws Z3Exception {
		// control flow constraints
		BoolExpr nodePCVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		AbstractPDGNode calleeNode = null;
		if (isReturnNode(node, pdg))
			calleeNode = getCrossFunctionNode(node, pdg, true);
		else if (isCallerNode(node, pdg))
			calleeNode = getCrossFunctionNode(node, pdg, false);
		BoolExpr calleeNodePCVar = getOrAddVar(pdgNodeToZ3Var, calleeNode.getNodeId(), ctx);
		constraints.add(ctx.MkEq(nodePCVar, calleeNodePCVar));
		
		// expression constraints
		if (!isExprNode(node)) return;
		
		BoolExpr target = null;
		if (isReturnNode(node, pdg))
			target = nodePCVar;
		else if (isCallerNode(node, pdg))
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
		if (origEqExp != null && isExprNode(node)) {
			origFuncConstraint = Z3Addons.removeConstraintContainingExp(origFuncConstraint, origEqExp, ctx);
		}
		if (origEqExp != null && !isExprNode(node)) {
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
		BoolExpr nodePCVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
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
		String functionName = getFunctionNameForCall(node, pdg);
		
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
		Set<Integer> labels = getAllSiteLabels(nodes, pdg);
		
		// assemble constraints
		BoolExpr fullConstraint = null;
		for (int labelId : labels) {
			BoolExpr constraintPerLabel = null;
			for (AbstractPDGNode node : nodes) {
				AbstractPDGNode sourceLabel = getLabelPredecessor(node, pdg, labelId);
				int nodeId = node.getNodeId();
				int sourceId = sourceLabel.getNodeId();
				// PC constraint
				BoolExpr nodePCVar = getOrAddVar(pdgNodeToZ3Var, nodeId, ctx);
				BoolExpr sourceLabelPCVar = getOrAddVar(pdgNodeToZ3Var, sourceId, ctx);
				BoolExpr pcConstraint = ctx.MkEq(nodePCVar, sourceLabelPCVar);
				constraintPerLabel = Z3Addons.andConstraints(constraintPerLabel, ctx, pcConstraint);
				
				// Exp constraint
				if (isExprNode(node)) {
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

	public static void getNodeConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
									Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var, 
									Set<BoolExpr> constraints, 
									Map<String, BoolExpr> funcToConstraint) throws Z3Exception {
		if (isReturnNode(node, pdg)) {
			Set<AbstractPDGNode> nodes = getFunctionCallNodes(node, pdg);
			Set<BoolExpr> newConstraints = new LinkedHashSet<>();
			for (AbstractPDGNode cur : nodes) {
				getFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, newConstraints);					
				getNonFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
			}
			updateFuncConstraint(node, pdg, newConstraints, funcToConstraint, ctx, pdgNodeToZ3Var);
		} else if (isEntryNode(node, pdg) && !isMainEntry(node, pdg) &&
						!funcToConstraint.containsKey(node.getProcedureName())) {
			Set<AbstractPDGNode> nodes = getEntryNodes(node, pdg);
			getEntryNodeConstraints(nodes, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
		}
		else {
			getNonFunctionConstraints(node,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
		}	
	}
	
	public static void addIfNotVisited(AbstractPDGNode node, Set<Integer> visited,
									Deque<Integer> workQueue) {
		int nodeId = node.getNodeId();
		if (visited.add(nodeId)) {
			workQueue.add(nodeId);
		}
	}

	public static void getPredecessors(AbstractPDGNode node, 
								ProgramDependenceGraph pdg, Set<Integer> visited, 
								Deque<Integer> workQueue, 
								Map<String, BoolExpr> funcToConstraint) {
		if (debugMode) System.out.println();

		if (isMainEntry(node, pdg)) return;
		
		// Only add entry node's parent if not in funcToConstraint
		String functionName = node.getProcedureName();
		if (isEntryNode(node, pdg) && funcToConstraint.containsKey(functionName))
			return;
		
		// If this node is part of a function call in the caller, all nodes involved with
		// the call were processed at once. Don't add the other nodes within the caller
		// involved with this function call. 
		if (isReturnNode(node, pdg)) {
			Set<AbstractPDGNode> nodes = getFunctionCallNodes(node, pdg);
			Set<AbstractPDGNode> predecessors = new HashSet<AbstractPDGNode>();
			if (debugMode) System.out.println("Function call nodes:");
			for (AbstractPDGNode funcCallNode : nodes) {
				if (debugMode) System.out.println(funcCallNode.getName());
				if (isMainEntry(funcCallNode, pdg)) continue;
				for (PDGEdge edge : pdg.incomingEdgesOf(funcCallNode)) {
					predecessors.add(edge.getSource());
				}
			}
			predecessors.removeAll(nodes);
			if (debugMode) System.out.println("Predecessors:");
			for (AbstractPDGNode predecessor : predecessors) {
				if (debugMode) System.out.println(predecessor.getName());
				addIfNotVisited(predecessor, visited, workQueue);
			}
			if (debugMode) System.out.println();
			return;
		}
		
		// For other nodes, add everything
		if (debugMode) System.out.println("Predecessors:");
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			if (debugMode) System.out.println(edge.getSource().getName());
			addIfNotVisited(edge.getSource(), visited, workQueue);
		}
		if (debugMode) System.out.println();
	}
	
	public static Set<BoolExpr> getConstraints(int nodeID, ProgramDependenceGraph pdg, 
														Context ctx) 
														throws Z3Exception {
		return getConstraints(nodeID, pdg, ctx, new HashSet<Integer>(), 
												new HashMap<Integer, BoolExpr>(),
												new HashMap<Integer, Expr>(),
												new HashMap<String, BoolExpr>());
	}
	
	public static Set<BoolExpr> getConstraints(int nodeID, ProgramDependenceGraph pdg, 
												Context ctx, Set<Integer> visited, 
												Map<Integer, BoolExpr> pdgNodeToZ3Var, 
												Map<Integer, Expr> expNodeToZ3Var,
												Map<String, BoolExpr> funcToConstraint) 
												throws Z3Exception {
		Deque<Integer> workQueue = new ArrayDeque<>();
		Set<BoolExpr> constraints = new LinkedHashSet<>();
		
		BoolExpr base = Z3Addons.getFreshBoolVar(ctx);
		workQueue.add(nodeID);
		visited.add(nodeID);
		pdgNodeToZ3Var.put(nodeID, base);
		constraints.add(base);
		
		while (!workQueue.isEmpty()) {
			Integer nextID = workQueue.remove();
			AbstractPDGNode node = pdg.getNodeById(nextID);
			System.out.println("Node being processed: " + node.getName() + " " + node.getJavaType());
			
			if (isMainEntry(node, pdg)) {
				// prune pc summary in MAIN, we don't need to go further back.
				// this is for debugging, shouldn't be needed later
				continue;
			}
			
			getNodeConstraints(node, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints, funcToConstraint);
		
			// add predecessors that we care about to the work queue
			getPredecessors(node, pdg, visited, workQueue, funcToConstraint);
		}
		constraints.addAll(funcToConstraint.values());
		
		if (debugMode) printVars(pdgNodeToZ3Var, expNodeToZ3Var, pdg);
		return constraints; 
	}
	
	public static Set<BoolExpr> getConstraintsPath(ArrayList<Integer> ids, 
												ProgramDependenceGraph pdg, 
												Context ctx) 
												throws Z3Exception {
		Set<BoolExpr> constraints = new LinkedHashSet<BoolExpr>();
		Set<Integer> visited = new HashSet<Integer>();
		Map<Integer, BoolExpr> pdgNodeToZ3Var = new HashMap<Integer, BoolExpr>();
		Map<Integer, Expr> expNodeToZ3Var = new HashMap<Integer, Expr>();
		Map<String, BoolExpr> funcToConstraint = new HashMap<String, BoolExpr>();

		for (Integer id : ids) {
			Set<BoolExpr> newConstraints = getConstraints(id, pdg, ctx, visited, 
									pdgNodeToZ3Var, expNodeToZ3Var, funcToConstraint);
			constraints.addAll(newConstraints);
		}

		return constraints;
	}
	
	public static void printVars(Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var,
									ProgramDependenceGraph pdg) {
		System.out.println();
		System.out.println("Node Z3 Variables:");
		System.out.println("PC Vars:");
		for (int id : pdgNodeToZ3Var.keySet()) {
			AbstractPDGNode node = pdg.getNodeById(id);
			System.out.println(pdgNodeToZ3Var.get(id) + " " + node.getName());
		}
		System.out.println("Exp Vars:");
		for (int id : expNodeToZ3Var.keySet()) {
			AbstractPDGNode node = pdg.getNodeById(id);
			System.out.println(expNodeToZ3Var.get(id) + " " + node.getName());
		}
		System.out.println();
	}
	
	public static void printGraphInfo(ProgramDependenceGraph pdg) {
		Set<AbstractPDGNode> nodes = pdg.vertexSet();
		for (AbstractPDGNode node : nodes) {
			System.out.println(node.getName() + " " + node.getNodeType() + " " + node.getJavaType() + " " + node.getNodeId());
		}
	}
	
	public static void printConstraints(Set<BoolExpr> constraints) {
		for (BoolExpr expr : constraints) {
			System.out.println(expr);
		}
	}
	
	public static void getAndCheckConstraints(ProgramDependenceGraph pdg, int id) throws Z3Exception {
		Context ctx = new Context();
		Set<BoolExpr> constraints = getConstraints(id, pdg, ctx);
		printConstraints(constraints);
		System.out.println();
		Model model = ConstraintCheck.Check(ctx, constraints);
		System.out.println(model);		
	}
	
	public static void getAndCheckConstraints(ProgramDependenceGraph pdg, ArrayList<Integer> ids)
																throws Z3Exception {
		Context ctx = new Context();
		Set<BoolExpr> constraints = getConstraintsPath(ids, pdg, ctx);
		printConstraints(constraints);
		System.out.println();
		Model model = ConstraintCheck.Check(ctx, constraints);
		System.out.println(model);	
	}
	
	public static List<Integer> findMatchingNodeIds(ProgramDependenceGraph pdg, String phrase) {
		List<Integer> matchingIds = new ArrayList<Integer>();
		Set<AbstractPDGNode> nodes = pdg.vertexSet();
		for (AbstractPDGNode node : nodes) {
			if (node.getName().indexOf(phrase) != -1) {
				matchingIds.add(node.getNodeId());
			}
		}
		if (debugMode) {
			for (int id : matchingIds) {
				System.out.println(id);
			}
		}
		return matchingIds;
	}
	
	public static void main(String[] args) throws Z3Exception {
		String filename = "/Users/ramyarangan/Dropbox/Research/PLResearch/eclipseworkspace/accrue-bytecode/tests";
		filename += "/pdg_test.pointer.CallTwiceSimple.json.gz";
		ProgramDependenceGraph pdg = PDGFactory.graphFromJSONFile(filename, false);
		findMatchingNodeIds(pdg, "b = 1");
		//getAndCheckConstraints(pdg, 44);
	}
}
