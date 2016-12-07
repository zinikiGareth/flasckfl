package test.flas.testrunner;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.junit.Test;
import org.zinutils.utils.MultiTextEmitter;

public class SimpleUnitTestRunnerTests {

	@Test
	public void testItCanTestASimpleValue() throws Exception {
		File f = createFile("\tvalue x", "\t\t32");
		StringWriter sw = new StringWriter();
		UnitTestRunner r = new UnitTestRunner(new MultiTextEmitter(sw), new FLASCompiler(), f);
		r.run();
		assertEquals("PASSED:\tvalue x\n", sw.toString());
	}

	private File createFile(String... lines) throws IOException {
		File ret = File.createTempFile("testFor", ".ut");
		PrintWriter pw = new PrintWriter(ret);
		for (String s : lines)
			pw.println(s);
		pw.close();
		ret.deleteOnExit();
		return ret;
	}

}
