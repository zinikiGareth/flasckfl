package org.flasck.flas.compiler;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.debug.PFDumper;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.zinutils.utils.Indenter;

public class ActualPhase2Processor implements Phase2Processor {
	private final ErrorReporter errors;
	private final FLASCompiler compiler;
	private final Scope scope;
	
	public ActualPhase2Processor(ErrorReporter errors, @Deprecated /* the code called should come here or downstream */ FLASCompiler compiler, String inPkg) {
		this.errors = errors;
		this.compiler = compiler;
		this.scope = new Scope(new PackageName(inPkg));
	}
	
	@Override
	public SolidName qualifyName(String base) {
		return new SolidName(scope.scopeName, base);
	}

	@Override
	public Scope grabScope() {
		return scope;
	}

	@Override
	public void functionCase(FunctionCaseDefn fn) {
		int caseName = scope.caseName(fn.intro.name().uniqueName());
		fn.provideCaseName(caseName);
		scope.define(fn.functionName().name, fn);
	}

	@Override
	public void newStruct(StructDefn sd) {
		scope.define(sd.structName.baseName(), sd);
	}

	@Override
	public void process() {
		new PFDumper().dumpScope(new Indenter(new PrintWriter(System.out)), scope);
		try {
			compiler.stage2(errors, null, scope.scopeName.uniqueName(), scope);
		} catch (Throwable t) {
			t.printStackTrace();
			errors.message(((InputPosition)null), t.toString());
		}
	}

}
