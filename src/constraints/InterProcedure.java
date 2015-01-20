package constraints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Z3Exception;

public class InterProcedure {
	static final boolean debugMode = true;
		
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
	
	public static void getFunctionConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
												Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var,
												Map<Integer, Expr> expNodeToZ3Var,
												Set<BoolExpr> constraints) 
													throws Z3Exception {
		// control flow constraints
		BoolExpr nodePCVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		AbstractPDGNode calleeNode = null;
		if (PDGHelper.isReturnNode(node, pdg))
			calleeNode = PDGHelper.getCrossFunctionNode(node, pdg, true);
		else if (PDGHelper.isCallerNode(node, pdg))
			calleeNode = PDGHelper.getCrossFunctionNode(node, pdg, false);
		BoolExpr calleeNodePCVar = getOrAddVar(pdgNodeToZ3Var, calleeNode.getNodeId(), ctx);
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
				BoolExpr nodePCVar = getOrAddVar(pdgNodeToZ3Var, nodeId, ctx);
				BoolExpr sourceLabelPCVar = getOrAddVar(pdgNodeToZ3Var, sourceId, ctx);
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

	public static void getNodeConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
									Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var, 
									Set<BoolExpr> constraints, 
									Map<String, BoolExpr> funcToConstraint) throws Z3Exception {
		if (PDGHelper.isReturnNode(node, pdg)) {
			Set<AbstractPDGNode> nodes = PDGHelper.getFunctionCallNodes(node, pdg);
			Set<BoolExpr> newConstraints = new LinkedHashSet<>();
			for (AbstractPDGNode cur : nodes) {
				getFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, newConstraints);					
				IntraProcedure.getNonFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
			}
			updateFuncConstraint(node, pdg, newConstraints, funcToConstraint, ctx, pdgNodeToZ3Var);
		} else if (PDGHelper.isEntryNode(node, pdg) && !PDGHelper.isMainEntry(node, pdg) &&
						!funcToConstraint.containsKey(node.getProcedureName())) {
			Set<AbstractPDGNode> nodes = PDGHelper.getEntryNodes(node, pdg);
			getEntryNodeConstraints(nodes, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
		}
		else {
			IntraProcedure.getNonFunctionConstraints(node,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
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

		if (PDGHelper.isMainEntry(node, pdg)) return;
		
		// Only add entry node's parent if not in funcToConstraint
		String functionName = node.getProcedureName();
		if (PDGHelper.isEntryNode(node, pdg) && funcToConstraint.containsKey(functionName))
			return;
		
		// If this node is part of a function call in the caller, all nodes involved with
		// the call were processed at once. Don't add the other nodes within the caller
		// involved with this function call. 
		if (PDGHelper.isReturnNode(node, pdg)) {
			Set<AbstractPDGNode> nodes = PDGHelper.getFunctionCallNodes(node, pdg);
			Set<AbstractPDGNode> predecessors = new HashSet<AbstractPDGNode>();
			if (debugMode) System.out.println("Function call nodes:");
			for (AbstractPDGNode funcCallNode : nodes) {
				if (debugMode) System.out.println(funcCallNode.getName());
				if (PDGHelper.isMainEntry(funcCallNode, pdg)) continue;
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
			
			if (PDGHelper.isMainEntry(node, pdg)) {
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
