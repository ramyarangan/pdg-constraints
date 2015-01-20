package constraints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.PDGEdgeType;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;
import accrue.pdg.node.PDGNodeType;
import accrue.pdg.util.CallSiteEdgeLabel;
import accrue.pdg.util.CallSiteEdgeLabel.SiteType;

public class PDGHelper {

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

}
