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
	
	public static void main(String[] args) throws Z3Exception {
		testUnseenAnd();
	}
}
