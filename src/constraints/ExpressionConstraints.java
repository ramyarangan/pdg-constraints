package constraints;

import java.util.Map;
import java.util.Set;

import accrue.pdg.PDGEdge;
import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.node.AbstractPDGNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

public class ExpressionConstraints {
	public static String getVarName(String name) {
		int eqIdx = name.indexOf("=");
		if (eqIdx != -1) 
			return name.substring(0,eqIdx);
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
	
	public static BoolExpr getConst(String name, Context ctx) throws Z3Exception {
		if (name.equals("1")) return ctx.MkTrue();
		if (name.equals("0")) return ctx.MkFalse();
		return null;
	}

	// searches for node with name "name" amongst the parents of this node in the z3var map
	public static BoolExpr getZ3VarFromSources(String name, AbstractPDGNode node, ProgramDependenceGraph pdg, 
									Map<Integer,BoolExpr> pdgNodeToZ3Var, Context ctx) throws Z3Exception {
		Set<PDGEdge> edges = pdg.incomingEdgesOf(node); 
		for (PDGEdge edge : edges) {
			AbstractPDGNode source = edge.getSource();
			String sourceName = getVarName(source.getName());
			if (name.equals(sourceName)) {
				return IntraProcedureConstraints.getOrAddVar(pdgNodeToZ3Var, source.getNodeId(), ctx);
			}
		}
		// this should never happen
		return Z3Addons.getFreshBoolVar(ctx);
	}
	
	public static BoolExpr getExpConstraint(AbstractPDGNode node, ProgramDependenceGraph pdg, 
											Map<Integer, BoolExpr> pdgNodeToZ3Var, Context ctx) 
															throws Z3Exception {
		String name = node.getName();
		// deal with LOCAL nodes first
		if (name.indexOf("=") != -1) {
			name = name.substring(name.indexOf("=") + 2);
		}
		
		// < case
		// > case
		// ! case
		// phi case
		
		// unary case
		BoolExpr constExpr = getConst(name, ctx);
		if (constExpr != null) return constExpr;
		return getZ3VarFromSources(name, node, pdg, pdgNodeToZ3Var, ctx);
	}
}
