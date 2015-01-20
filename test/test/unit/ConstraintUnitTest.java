package test.unit;

import accrue.pdg.ProgramDependenceGraph;
import accrue.pdg.graph.PDGFactory;

import com.microsoft.z3.Z3Exception;

import constraints.InterProcedure;

public class ConstraintUnitTest {
	private static String filename = "/Users/ramyarangan/Dropbox/Research/PLResearch/eclipseworkspace/pdg-constraints/tests";

	public static ProgramDependenceGraph getPDGJSON(String testFile) {
		String fullFilename = filename + testFile;
		return PDGFactory.graphFromJSONFile(fullFilename, false);
	}
	
	public static void testAndPrintConstraints(String filename, String phrase,
													int id, boolean test) throws Z3Exception {
		ProgramDependenceGraph pdg = getPDGJSON(filename);
		InterProcedure.findMatchingNodeIds(pdg, phrase);
		if (test) InterProcedure.getAndCheckConstraints(pdg, id);
	}
	
	public static void testSeen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.Seen.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 40, true);
	}
	
	public static void testUnseen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.Unseen.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 40, true);
	}
	
	public static void testSeenAnd() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.SeenAnd.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 47, true);
	}
	
	public static void testUnseenAnd() throws Z3Exception {
		String testFile = "/pdg_test.constraints.basic.UnseenAnd.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 47, true);
	}
	
	public static void testBool1() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Bool1.json.gz";
		String phrase = "z = 0";
		testAndPrintConstraints(testFile, phrase, 37, true);
	}
	
	public static void testBool2() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Bool2.json.gz";
		String phrase = "return 0";
		testAndPrintConstraints(testFile, phrase, 47, true);
	}
	
	public static void testInt1() throws Z3Exception {
		String testFile = "/pdg_test.constraints.expression.Int1.json.gz";
		String phrase = "!(5 >= 4)";
		testAndPrintConstraints(testFile, phrase, 30, true);
	}
	
	public static void testOneCallNoArgs() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.OneCallNoArgs.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 45, true);
	}
	
	public static void testOneCallOneArg() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.OneCallOneArg.json.gz";
		String phrase = "y = 0";
		testAndPrintConstraints(testFile, phrase, 47, true);
	}
	
	public static void testMultipleCallDisjunction() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallDisjunction.json.gz";
		String phrase = "x = 0";
		testAndPrintConstraints(testFile, phrase, 65, true);
	}
	
	public static void testMultipleCallImprecision1() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallImprecision1.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 61, true);
	}
	
	public static void testMultipleCallImprecision2() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallImprecision2.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 48, true);
	}
	
	public static void testMultipleCallImprecision3() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallImprecision3.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 59, true);
	}
	
	public static void testMultipleCallSeen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallSeen.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 60, true);
	}
	
	public static void testMultipleCallUnseen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.interprocedural.MultipleCallUnseen.json.gz";
		String phrase = "y = 1";
		testAndPrintConstraints(testFile, phrase, 60, true);
	}
	
	public static void testWhileLoopUnseen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.loop.WhileLoopUnseen.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 44, true);
	}
	
	
	/**
	 * Test fails, expectedly. We don't go through the while loop multiple times.
	 * 
	 * @throws Z3Exception
	 */
	public static void testWhileLoopSeen() throws Z3Exception {
		String testFile = "/pdg_test.constraints.loop.WhileLoopSeen.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 45, true);
	}
	
	public static void testWhileLoopImprecision() throws Z3Exception {
		String testFile = "/pdg_test.constraints.loop.WhileLoopImprecision.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 44, true);
	}
	
	/**
	 * Test fails - I'm not handling phi nodes correctly when multiple 
	 * expression nodes copy their values from a phi node. Here the phi node
	 * of the while loop isn't able to be both the original value at one point
	 * and the new value at another point 
	 *  
	 * @throws Z3Exception
	 */
	public static void testWhileLoopSeenNow() throws Z3Exception {
		String testFile = "/pdg_test.constraints.loop.WhileLoopSeenNow.json.gz";
		String phrase = "x = 1";
		testAndPrintConstraints(testFile, phrase, 45, true);
	}
	
	public static void testSimplePassword() throws Z3Exception {
		String testFile = "/pdg_test.integration.SimplePassword.json.gz";
		String phrase = "selfDestruct = 1";
		testAndPrintConstraints(testFile, phrase, 127, true);
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
		testMultipleCallDisjunction();
		testMultipleCallImprecision1();
		testMultipleCallImprecision2();
		testMultipleCallImprecision3();
		testMultipleCallSeen();
		testMultipleCallUnseen();
	}

	public static void testLoop() throws Z3Exception {
		testWhileLoopUnseen();
		testWhileLoopSeen();
		testWhileLoopSeenNow();
		testWhileLoopImprecision();
	}
	
	public static void testIntegration() throws Z3Exception {
		testSimplePassword();
	}
	
	public static void main(String[] args) throws Z3Exception {
		testSimplePassword();
	}
}
