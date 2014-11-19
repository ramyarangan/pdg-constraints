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
import accrue.pdg.PDGEdgeType;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;
import accrue.pdg.node.AbstractPDGNode;
import accrue.pdg.node.PDGNodeType;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Z3Exception;

public class IntraProcedureConstraints {
	
	public static BoolExpr getOrAddVar(Map<Integer, BoolExpr> pdgNodeToZ3Var, int id, Context ctx) 
				throws Z3Exception {
		BoolExpr nodeVar = null;
		if (pdgNodeToZ3Var.containsKey(id)) {
			nodeVar = pdgNodeToZ3Var.get(id);
		}
		else {
			nodeVar = Z3Addons.getFreshBoolVar(ctx);
			pdgNodeToZ3Var.put(id, nodeVar);
		}
		return nodeVar;
	}
	
	// the following methods should probably be in the node and edge classes?
	public static boolean isExprNode(AbstractPDGNode node) {
		switch (node.getNodeType()) {
			case LOCAL:
			case OTHER_EXPRESSION:
				return true;
			default:
				return false;
		}
	}
	
	public static boolean isPCNode(AbstractPDGNode node) {
		switch(node.getNodeType()) {
			case BOOLEAN_FALSE_PC:
			case BOOLEAN_TRUE_PC:
			case PC_MERGE:
			case PC_OTHER:
				return true;
			default:
				return false;
		}
	}
	
	public static AbstractPDGNode getSourceNodeByType(List<PDGEdge> edgeList, PDGEdgeType type) {
		for (PDGEdge edge : edgeList) {
			if (edge.getType() == type)
				return edge.getSource();
		}
		return null;
	}
	
	/**
	 * XXX
	 * 
	 * @param node
	 * @param pdg
	 * @param ctx
	 * @param pdgNodeToZ3Var
	 * @return
	 * @throws Z3Exception
	 */
	public static void getControlFlowConstraints(AbstractPDGNode node, ProgramDependenceGraph pdg, 
													Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var,
													Set<BoolExpr> newConstraints) 
													throws Z3Exception {		
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node);
		if (edges.isEmpty()) return;
		
		List<PDGEdge> edgeList = new ArrayList<PDGEdge>(edges);
		BoolExpr pcConstraint = null;
		
		// merge type
		AbstractPDGNode mergeNode = getSourceNodeByType(edgeList,PDGEdgeType.MERGE);
		if (mergeNode != null) {
			mergeNode = edgeList.get(0).getSource();
			pcConstraint = getOrAddVar(pdgNodeToZ3Var, mergeNode.getNodeId(), ctx);
			for (int i = 1; i < edgeList.size(); i++) {
				assert(edgeList.get(i).getType() == PDGEdgeType.MERGE);
				mergeNode = edgeList.get(i).getSource();
				BoolExpr newConstraint = getOrAddVar(pdgNodeToZ3Var, mergeNode.getNodeId(), ctx);
				pcConstraint = ctx.MkOr(new BoolExpr[] {pcConstraint, newConstraint});
			}
		}
		
		// true type
		AbstractPDGNode trueNode = getSourceNodeByType(edgeList,PDGEdgeType.TRUE);
		if (trueNode != null) {
			BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, trueNode.getNodeId(), ctx);
			BoolExpr nodeConstraint = ExpressionConstraints.getExpConstraint(trueNode, pdg, pdgNodeToZ3Var, ctx);
			constraints.add(ctx.MkEq(nodeVar, nodeConstraint));
			pcConstraint = nodeVar;
		}
		
		// false type
		AbstractPDGNode falseNode = getSourceNodeByType(edgeList,PDGEdgeType.FALSE);
		if (falseNode != null) {
			BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, falseNode.getNodeId(), ctx);
			BoolExpr nodeConstraint = ExpressionConstraints.getExpConstraint(falseNode, pdg, pdgNodeToZ3Var, ctx);
			constraints.add(ctx.MkEq(nodeVar, nodeConstraint));
			pcConstraint = ctx.MkNot(nodeVar);
		}
		
		// implicit type
		AbstractPDGNode implicitNode = getSourceNodeByType(edgeList,PDGEdgeType.IMPLICIT);
		if (implicitNode != null) {
			pcConstraint = getOrAddVar(pdgNodeToZ3Var, implicitNode.getNodeId(), ctx);
		}
		
		// conjunction type
		AbstractPDGNode conjunctionNode = getSourceNodeByType(edgeList, PDGEdgeType.CONJUNCTION);
		if (conjunctionNode != null) {
			AbstractPDGNode addedNode = null;
			if (pcConstraint == null) {
				pcConstraint = getOrAddVar(pdgNodeToZ3Var, conjunctionNode.getNodeId(), ctx);
				addedNode = conjunctionNode;
			}
			for (PDGEdge edge : edgeList) {
				AbstractPDGNode source = edge.getSource();
				if ((edge.getType() == PDGEdgeType.CONJUNCTION) && (!source.equals(addedNode))) {
					BoolExpr newVar = getOrAddVar(pdgNodeToZ3Var, source.getNodeId(), ctx);
					pcConstraint = ctx.MkAnd(new BoolExpr[] {newVar, pcConstraint});
				}
			}
		}
		constraints.add(pcConstraint);
		
		return;
	}
	
	public static Deque<Integer> getPredecessors(AbstractPDGNode node, 
								ProgramDependenceGraph pdg, Set<Integer> visited) {
		Deque<Integer> workQueue = new ArrayDeque<>();
		if (isPCNode(node)) {
			for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
				int sourceId = edge.getSource().getNodeId();
				// bool_true_pc shouldn't add the parent vertex to the traversal
				if (isPCNode(edge.getSource()) && visited.add(sourceId)) {
					workQueue.add(sourceId);
				}
			}
			return workQueue;
		}
		
		// only add the PC node if there is a PC node
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			AbstractPDGNode source = edge.getSource();
			int sourceId = source.getNodeId();
			if (isPCNode(source)) {
				if (visited.add(sourceId)) {
					workQueue.add(sourceId);
				}
				return workQueue;
			}
		}
		// add all nodes otherwise
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			int sourceId = edge.getSource().getNodeId();
			if (visited.add(sourceId)) {
				workQueue.add(sourceId);
			}
		}
		return workQueue;
	}
	
	public static Set<BoolExpr> getIntraProcedureConstraints(int nodeID, ProgramDependenceGraph pdg, 
														Context ctx) throws Z3Exception {
		Deque<Integer> workQueue = new ArrayDeque<>();
		Set<Integer> visited = new HashSet<>();
		Set<BoolExpr> constraints = new LinkedHashSet<>();
		Map<Integer, BoolExpr> pdgNodeToZ3Var = new HashMap<>();
		Map<Integer, BoolExpr> expNodeToZ3Var = new HashMap<>();
		
		BoolExpr base = Z3Addons.getFreshBoolVar(ctx);
		workQueue.add(nodeID);
		visited.add(nodeID);
		pdgNodeToZ3Var.put(nodeID, base);
		constraints.add(base);
		
		while (!workQueue.isEmpty()) {
			Integer nextID = workQueue.remove();
			AbstractPDGNode node = pdg.getNodeById(nextID);
			System.out.println("Node being processed: " + node.getName());
			
			if (node.getNodeType() == PDGNodeType.ENTRY_PC_SUMMARY) {
				// prune pc summary, we don't need to go further back.
				continue;
			}
			
			Set<BoolExpr> newConstraints = new HashSet<BoolExpr>();
			getControlFlowConstraints(node, pdg, ctx, pdgNodeToZ3Var, newConstraints);
			constraints.addAll(newConstraints);
			
			if (isExprNode(node)) {
				// pretty sure you're never adding a new var here, check ltr
				BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, nextID, ctx);
				BoolExpr nodeConstraint = ExpressionConstraints.getExpConstraint(node, pdg, pdgNodeToZ3Var, ctx);
				constraints.add(ctx.MkEq(nodeVar, nodeConstraint));
			}
			
			// add predecessors that we care about to the work queue
			Deque<Integer> nextQueue = getPredecessors(node, pdg, visited);
			workQueue.addAll(nextQueue);
			
		}
		
		return constraints; 
	}
	
	public static void printGraphInfo(ProgramDependenceGraph pdg) {
		Set<AbstractPDGNode> nodes = pdg.vertexSet();
		for (AbstractPDGNode node : nodes) {
			System.out.println(node.getName() + " " + node.getNodeType() + " " + node.getNodeId());
		}
	}
	
	public static void printConstraints(Set<BoolExpr> constraints) {
		for (BoolExpr expr : constraints) {
			System.out.println(expr);
		}
	}
	
	public static void main(String[] args) throws Z3Exception {
		String filename = "/Users/ramyarangan/Dropbox/Research/PLResearch/eclipseworkspace/accrue-bytecode/tests";
		filename += "/pdg_test.instruction.Unseen.json.gz";
		ProgramDependenceGraph pdg = PDGFactory.graphFromJSONFile(filename, false);
		//printGraphInfo(pdg);
		Context ctx = new Context();
		Set<BoolExpr> constraints = getIntraProcedureConstraints(36, pdg, ctx);
		printConstraints(constraints);
		System.out.println();
		Model model = ConstraintCheck.Check(ctx, constraints);
		System.out.println(model);
	}
}
