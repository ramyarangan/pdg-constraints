package constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Z3Exception;

public class GraphConstraintInfo {

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
		Set<BoolExpr> constraints = PDGConstraint.getConstraints(id, pdg, ctx);
		printConstraints(constraints);
		System.out.println();
		Model model = ConstraintCheck.Check(ctx, constraints);
		System.out.println(model);		
	}

	public static void getAndCheckConstraints(ProgramDependenceGraph pdg, ArrayList<Integer> ids)
																throws Z3Exception {
		Context ctx = new Context();
		Set<BoolExpr> constraints = PDGConstraint.getConstraintsPath(ids, pdg, ctx);
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
		if (PDGConstraint.debugMode) {
			for (int id : matchingIds) {
				System.out.println(id);
			}
		}
		return matchingIds;
	}

}
