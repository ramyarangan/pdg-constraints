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
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Z3Exception;

public class IntraProcedureConstraints {
	public static Expr getOrAddAnyVar(Map<Integer, Expr> mapToZ3Var, int id, Context ctx) 
			throws Z3Exception {
		Expr nodeVar = null;
		if (mapToZ3Var.containsKey(id)) {
			nodeVar = mapToZ3Var.get(id);
		}
		else {
			nodeVar = Z3Addons.getFreshBoolVar(ctx);
			mapToZ3Var.put(id, nodeVar);
		}
		return nodeVar;
	}	
	
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
	
	// the following methods should probably be in the node and edge classes?
	public static boolean isExprNode(AbstractPDGNode node) {
		switch (node.getNodeType()) {
			case LOCAL:
			case BASE_VALUE:
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

	public static BoolExpr getMergeControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
					Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {
		BoolExpr pcConstraint = null;

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
		
		return pcConstraint;
	}
	
	public static BoolExpr getTrueControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		AbstractPDGNode trueNode = getSourceNodeByType(edgeList,PDGEdgeType.TRUE);
		if (trueNode != null) {
			return getOrAddVar(pdgNodeToZ3Var, trueNode.getNodeId(), ctx);
		}
		return null;
	}
	
	public static BoolExpr getFalseControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		AbstractPDGNode falseNode = getSourceNodeByType(edgeList,PDGEdgeType.FALSE);
		if (falseNode != null) {
			return getOrAddVar(pdgNodeToZ3Var, falseNode.getNodeId(), ctx);
		}
		return null;
	}	
	
	public static BoolExpr getCopyExplicitControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode copyExplicitNode = getSourceNodeByType(edgeList,PDGEdgeType.COPY);
		if (copyExplicitNode == null) getSourceNodeByType(edgeList,PDGEdgeType.EXP);
		if (copyExplicitNode != null) {
			for (PDGEdge edge : edgeList) {
				if ((edge.getType() == PDGEdgeType.COPY) || (edge.getType() == PDGEdgeType.EXP)) {
					copyExplicitNode = edge.getSource();
					BoolExpr newConstraint = getOrAddVar(pdgNodeToZ3Var, copyExplicitNode.getNodeId(), ctx);
					pcConstraint = addConstraints(pcConstraint, ctx, newConstraint);
				}
			}
		}
		
		return pcConstraint;
	}
	
	public static BoolExpr getImplicitControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		AbstractPDGNode implicitNode = getSourceNodeByType(edgeList,PDGEdgeType.IMPLICIT);
		if (implicitNode != null) {
			return getOrAddVar(pdgNodeToZ3Var, implicitNode.getNodeId(), ctx);
		}
		return null;
	}

	public static BoolExpr getConjunctionControlFlowConstraints(List<PDGEdge> edgeList, ProgramDependenceGraph pdg, 
			Context ctx, Map<Integer, BoolExpr> pdgNodeToZ3Var) throws Z3Exception {		
		BoolExpr pcConstraint = null;
		
		AbstractPDGNode conjunctionNode = getSourceNodeByType(edgeList, PDGEdgeType.CONJUNCTION);
		if (conjunctionNode != null) {
			for (PDGEdge edge : edgeList) {
				AbstractPDGNode source = edge.getSource();
				if (edge.getType() == PDGEdgeType.CONJUNCTION) {
					BoolExpr newVar = getOrAddVar(pdgNodeToZ3Var, source.getNodeId(), ctx);
					pcConstraint = addConstraints(pcConstraint, ctx, newVar);
				}
			}
		}
		
		return pcConstraint;
	}
	
	public static BoolExpr addConstraints(BoolExpr pcConstraint, Context ctx, BoolExpr addConstraint) 
														throws Z3Exception {
		if (pcConstraint != null) {
			if (addConstraint != null) {
				return ctx.MkAnd(new BoolExpr[] {pcConstraint, addConstraint});
			}
			return pcConstraint;
		}
		return addConstraint;
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
													Set<BoolExpr> constraints) 
													throws Z3Exception {
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node);
		if (edges.isEmpty()) return;
		
		List<PDGEdge> edgeList = new ArrayList<PDGEdge>(edges);
		BoolExpr pcConstraint = null;
		
		// merge type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getMergeControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));
		
		// true type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getTrueControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));

		// false type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getFalseControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));
	
		// copy and explicit type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getCopyExplicitControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));
		
		// implicit type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getImplicitControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));

		// conjunction type
		pcConstraint = addConstraints(pcConstraint, ctx,
				getConjunctionControlFlowConstraints(edgeList, pdg, ctx, pdgNodeToZ3Var));

		BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		if (pcConstraint != null)
			constraints.add(ctx.MkEq(nodeVar, pcConstraint));
	}
	
	public static BoolExpr getTrueFalseCondition(AbstractPDGNode node, 
												ProgramDependenceGraph pdg, Context ctx,
												Map<Integer, Expr> expNodeToZ3Var) throws Z3Exception{
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node);
		List<PDGEdge> edgeList = new ArrayList<PDGEdge>(edges);
		// true type
		AbstractPDGNode trueNode = getSourceNodeByType(edgeList,PDGEdgeType.TRUE);
		if (trueNode != null) {
			BoolExpr trueNodeVar = (BoolExpr) getOrAddAnyVar(expNodeToZ3Var, trueNode.getNodeId(), ctx);
			return trueNodeVar;
		}
		
		// false type
		AbstractPDGNode falseNode = getSourceNodeByType(edgeList,PDGEdgeType.FALSE);
		if (falseNode != null) {
			BoolExpr falseNodeVar = (BoolExpr) getOrAddAnyVar(expNodeToZ3Var, falseNode.getNodeId(), ctx);
			return ctx.MkNot(falseNodeVar);
		}	
		
		return null;
	}
	
	public static void getExpressionConstraints(AbstractPDGNode node, 
									ProgramDependenceGraph pdg, Context ctx, 
									Map<Integer, BoolExpr> pdgNodeToZ3Var, 
									Map<Integer, Expr> expNodeToZ3Var, 
									Set<BoolExpr> constraints) throws Z3Exception {
		BoolExpr expConstraint = null;
		
		BoolExpr trueFalseCondition = getTrueFalseCondition(node, pdg, ctx, expNodeToZ3Var);
		if (trueFalseCondition != null) expConstraint = trueFalseCondition;
		
		if (isExprNode(node)) {
			// expression constraint should be something like this val constraint = 
			// some combination of parent's val constraints.
			BoolExpr nodeConstraint = ExpressionConstraints.getExpConstraint(node, pdg, expNodeToZ3Var, ctx);
			if (nodeConstraint != null) {
				expConstraint = nodeConstraint;
				System.out.println("Node constraint for " + node.getName() + " " + expConstraint);
			}
		}
		
		BoolExpr nodeVar = getOrAddVar(pdgNodeToZ3Var, node.getNodeId(), ctx);
		if (expConstraint != null)
			constraints.add(ctx.MkImplies(nodeVar, expConstraint));
	}
	
	public static void getPredecessors(AbstractPDGNode node, 
								ProgramDependenceGraph pdg, Set<Integer> visited, 
								Deque<Integer> workQueue) {
		for (PDGEdge edge : pdg.incomingEdgesOf(node)) {
			int sourceId = edge.getSource().getNodeId();
			if (visited.add(sourceId)) {
				workQueue.add(sourceId);
			}
		}
	}
	
	public static Set<BoolExpr> getIntraProcedureConstraints(int nodeID, ProgramDependenceGraph pdg, 
														Context ctx) throws Z3Exception {
		Deque<Integer> workQueue = new ArrayDeque<>();
		Set<Integer> visited = new HashSet<>();
		Set<BoolExpr> constraints = new LinkedHashSet<>();
		Map<Integer, BoolExpr> pdgNodeToZ3Var = new HashMap<>();
		Map<Integer, Expr> expNodeToZ3Var = new HashMap<>();
		
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
			
			getControlFlowConstraints(node, pdg, ctx, pdgNodeToZ3Var, constraints);
			getExpressionConstraints(node, pdg, ctx, pdgNodeToZ3Var, expNodeToZ3Var, constraints);
			
			// add predecessors that we care about to the work queue
			getPredecessors(node, pdg, visited, workQueue);
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
//		 filename += "/pdg_test.instruction.UnseenAnd.json.gz";
		filename += "/pdg_test.pointer.CallTwice.json.gz";
		ProgramDependenceGraph pdg = PDGFactory.graphFromJSONFile(filename, false);
		// test: Unseen/ Seen: 36
		// 		 Phi1: 35
		//  	UnseenAnd/ SeenAnd : 43
		printGraphInfo(pdg);
//		Context ctx = new Context();
//		Set<BoolExpr> constraints = getIntraProcedureConstraints(43, pdg, ctx);
//		printConstraints(constraints);
//		System.out.println();
//		Model model = ConstraintCheck.Check(ctx, constraints);
//		System.out.println(model);
	}
}
