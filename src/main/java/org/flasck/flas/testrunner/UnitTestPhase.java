package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.compiler.UnitTestTranslator;
import org.flasck.flas.debug.PFDumper;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.ziniki.cbstore.json.FLConstructorServer;
import org.zinutils.utils.Indenter;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestPhase implements UnitTestTranslator {
	private final ErrorReporter errors;
	private final TopLevelDefnConsumer sb;
	private final List<File> tests = new ArrayList<>();

	public UnitTestPhase(ErrorReporter errors, TopLevelDefnConsumer sb) {
		this.errors = errors;
		this.sb = sb;
	}

	@Override
	public void process(File f) {
		tests.add(f);
		final String packageName = f.getName()+"._ut";
		if (FLASCompiler.backwardCompatibilityMode) {
			TestScript script = UnitTestRunner.convertScript(errors, sb.grabScope(), packageName, f);
			if (errors.hasErrors())
				return;
			System.out.println("==== UT " + packageName + " ====");
			new PFDumper().dumpScope(new Indenter(new PrintWriter(System.out)), script.scope());
			System.out.println("========================");
		} else {
			throw new org.zinutils.exceptions.NotImplementedException();
		}
	}

	public void runTests(boolean unitjvm, boolean unitjs, File writeTestReports, List<File> utpaths) {
		for (File f : tests) {
			MultiTextEmitter results = null;
			boolean close = false;
			try {
				if (writeTestReports != null && writeTestReports.isDirectory()) {
					results = new MultiTextEmitter(new File(writeTestReports, f.getName().replaceFirst(".ut$", ".txt")));
					close = true;
				} else {
					results = new MultiTextEmitter(System.out);
					close = false;
				}
	
				runTest(unitjvm, unitjs, results, utpaths, f);
			} catch (Exception ex) {
				errors.message(((InputPosition)null), ex.toString());
			} finally {
				if (close && results != null)
					results.close();
			}
		}
	}
	
	public void runTest(boolean unitjvm, boolean unitjs, MultiTextEmitter results, List<File> utpaths, File f) throws ClassNotFoundException, IOException, ErrorResultException {
		ScriptCompiler sc = null;
		CompileResult cr = null;
		UnitTestRunner utr = new UnitTestRunner(errors, sc, cr);
		utr.sendResultsTo(new FileUnitTestResultHandler(results));
		
		// We presumably needs some set of options to say which runners
		// we want to execute - could be more than one
		if (unitjvm) {
			JVMRunner jvmRunner = new JVMRunner(cr, new FLConstructorServer(cr.bce.getClassLoader()));
			for (File p : utpaths)
				jvmRunner.considerResource(p);
			utr.run(f, jvmRunner);
		}
		if (unitjs) {
			JSRunner jsRunner = new JSRunner(cr);
			utr.run(f, jsRunner);
		}
	}

}
