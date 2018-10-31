package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.UnitTestTranslator;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.ziniki.cbstore.json.FLConstructorServer;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestPhase implements UnitTestTranslator {
	private final ErrorReporter errors;
	private final TopLevelDefnConsumer sb;
	private final List<File> tests = new ArrayList<>();
	private final Map<String, TestScript> scripts = new HashMap<>();

	public UnitTestPhase(ErrorReporter errors, TopLevelDefnConsumer sb) {
		this.errors = errors;
		this.sb = sb;
	}

	@Override
	public void process(File f) {
		tests.add(f);
		if (FLASCompiler.backwardCompatibilityMode) {
			final Scope scope = sb.grabScope();
			final String packageName = scope.scopeName.uniqueName()+"._ut";
			TestScript script = UnitTestRunner.convertScript(errors, scope, packageName, f);
			if (errors.hasErrors())
				return;
			/*
			System.out.println("==== UT " + packageName + " ====");
			new PFDumper().dumpScope(new Indenter(new PrintWriter(System.out)), script.scope());
			System.out.println("========================");
			*/
			scripts.put(f.getName(), script);
		} else {
			throw new org.zinutils.exceptions.NotImplementedException();
		}
	}

	public void runTests(boolean unitjvm, boolean unitjs, File writeTestReports, List<File> utpaths, BCEClassLoader bce, Iterable<File> jsFiles) {
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
	
				runTest(unitjvm, unitjs, results, utpaths, bce, jsFiles, f);
			} catch (Exception ex) {
				errors.message(((InputPosition)null), ex.toString());
				if (ex instanceof NullPointerException || ex instanceof ClassNotFoundException)
					ex.printStackTrace();
			} finally {
				if (close && results != null)
					results.close();
			}
		}
	}
	
	public void runTest(boolean unitjvm, boolean unitjs, MultiTextEmitter results, List<File> utpaths, BCEClassLoader bce, Iterable<File> jsFiles, File f) throws ClassNotFoundException, IOException, ErrorResultException {
		UnitTestRunner utr = new UnitTestRunner(errors);
		utr.sendResultsTo(new FileUnitTestResultHandler(results));
		
		TestScript script = scripts.get(f.getName());
		// We presumably needs some set of options to say which runners
		// we want to execute - could be more than one
		if (unitjvm) {
			// cr, new FLConstructorServer(cr.bce.getClassLoader())
			JVMRunner jvmRunner = new JVMRunner(bce, new FLConstructorServer(bce), script.scope().scopeName.uniqueName(), script.getPriorScope(), script.getTestPackage());
			for (File p : utpaths)
				jvmRunner.considerResource(p);
			utr.run(jvmRunner, script);
		}
		if (unitjs) {
			JSRunner jsRunner = new JSRunner(script.scope().scopeName.uniqueName(), script.getPriorScope(), script.getTestPackage(), jsFiles);
			utr.run(jsRunner, script);
		}
	}
}
