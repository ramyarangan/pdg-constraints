package test.unit;

import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;

import com.microsoft.z3.Z3Exception;

import constraints.InterProcedureConstraints;

public class ConstraintUnitTest {
	private static String filename = "/Users/ramyarangan/Dropbox/Research/PLResearch/eclipseworkspace/pdg-constraints/tests";

	public static ProgramDependenceGraph getPDGJSON(String testFile) {
		String fullFilename = filename + testFile;
		return PDGFactory.graphFromJSONFile(fullFilename, false);
	}
	
	public static void testSeen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.Seen.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "x = 1");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 40);
	}
	
	public static void testUnseen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.Unseen.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "x = 1");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 40);
	}
	
	public static void testSeenAnd() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.SeenAnd.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "x = 1");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 47);
	}
	
	public static void testUnseenAnd() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.UnseenAnd.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "x = 1");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 47);
	}
	
	public static void testBool1() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Bool1.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "z = 0");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 37);
	}
	
	public static void testBool2() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Bool2.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "return 0");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 47);
	}
	
	public static void testInt1() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Int1.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "!(5 >= 4)");
		InterProcedureConstraints.getAndCheckConstraints(pdg,  30);
	}
	
	public static void testOneCallNoArgs() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.OneCallNoArgs.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "y = 1");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 45);
	}
	
	public static void testOneCallOneArg() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.OneCallOneArg.json.gz";
		ProgramDependenceGraph pdg = getPDGJSON(testFile);
		InterProcedureConstraints.findMatchingNodeIds(pdg, "y = 0");
		InterProcedureConstraints.getAndCheckConstraints(pdg, 47);
	}
	
	public static void testBasic() throws Z3Exception {
		testSeen();
		testUnseen();
		testSeenAnd();
		testUnseenAnd();
	}
	
	public static void testExpression() throws Z3Exception {
		testBool1();
		testBool2();
		testInt1();
	}
	
	public static void testInterprocedural() throws Z3Exception {
		testOneCallNoArgs();
		testOneCallOneArg();
	}
	
	public static void main(String[] args) throws Z3Exception {
		testInt1();
	}
}
