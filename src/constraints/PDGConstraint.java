package constraints;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class PDGConstraint {

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
	
	public static void getNodeConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg,
									Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var, 
									Set<BoolExpr> constraints, 
									Map<String, BoolExpr> funcToConstraint) throws Z3Exception {
		if (PDGHelper.isReturnNode(node, pdg)) {
			Set<AbstractPDGNode> nodes = PDGHelper.getFunctionCallNodes(node, pdg);
			Set<BoolExpr> newConstraints = new LinkedHashSet<>();
			for (AbstractPDGNode cur : nodes) {
				InterProcedure.getFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, newConstraints);					
				IntraProcedure.getNonFunctionConstraints(cur,  pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
			}
			InterProcedure.updateFuncConstraint(node, pdg, newConstraints, funcToConstraint, ctx, pdgNodeToZ3Var);
		} else if (PDGHelper.isEntryNode(node, pdg) && !PDGHelper.isMainEntry(node, pdg) &&
						!funcToConstraint.containsKey(node.getProcedureName())) {
			Set<AbstractPDGNode> nodes = PDGHelper.getEntryNodes(node, pdg);
			InterProcedure.getEntryNodeConstraints(nodes, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
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
		if (PDGConstraint.debugMode) System.out.println();
	
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
			if (PDGConstraint.debugMode) System.out.println("Function call nodes:");
			for (AbstractPDGNode funcCallNode : nodes) {
				if (PDGConstraint.debugMode) System.out.println(funcCallNode.getName());
				if (PDGHelper.isMainEntry(funcCallNode, pdg)) continue;
				for (PDGEdge edge : pdg.incomingEdgesOf(funcCallNode)) {
					predecessors.add(edge.getSource());
				}
			}
			predecessors.removeAll(nodes);
			if (PDGConstraint.debugMode) System.out.println("Predecessors:");
			for (AbstractPDGNode predecessor : predecessors) {
				if (PDGConstraint.debugMode) System.out.println(predecessor.getName());
				addIfNotVisited(predecessor, visited, workQueue);
			}
			if (PDGConstraint.debugMode) System.out.println();
			return;
		}
		
		// For other nodes, add everything
		if (PDGConstraint.debugMode) System.out.println("Predecessors:");
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			if (PDGConstraint.debugMode) System.out.println(edge.getSource().getName());
			addIfNotVisited(edge.getSource(), visited, workQueue);
		}
		if (PDGConstraint.debugMode) System.out.println();
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
		
		if (PDGConstraint.debugMode) GraphConstraintInfo.printVars(pdgNodeToZ3Var, expNodeToZ3Var, pdg);
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

	public static void main(String[] args) throws Z3Exception {
		String filename = "/Users/ramyarangan/Dropbox/Research/PLResearch/eclipseworkspace/accrue-bytecode/tests";
		filename += "/pdg_test.pointer.CallTwiceSimple.json.gz";
		ProgramDependenceGraph pdg = PDGFactory.graphFromJSONFile(filename, false);
		GraphConstraintInfo.findMatchingNodeIds(pdg, "b = 1");
		//getAndCheckConstraints(pdg, 44);
	}

	static final boolean debugMode = true;


}
