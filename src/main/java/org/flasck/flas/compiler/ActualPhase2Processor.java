package org.flasck.flas.compiler;

import java.io.File;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class ActualPhase2Processor implements Phase2Processor {
	private final ErrorReporter errors;
	private final FLASCompiler compiler;
	private final Scope scope;
	private ByteCodeEnvironment bce;
	private Iterable<File> jsFiles;
	
	public ActualPhase2Processor(ErrorReporter errors, @Deprecated /* the code called should come here or downstream */ FLASCompiler compiler, String inPkg) {
		this.errors = errors;
		this.compiler = compiler;
		this.scope = new Scope(new PackageName(inPkg));
	}
	
	@Override
	public SolidName qualifyName(String base) {
		return new SolidName(scope.scopeName, base);
	}

	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, scope.name(), base);
	}
	
	@Override
	public void functionIntro(FunctionIntro fn) {
		int caseName = scope.caseName(fn.name().uniqueName());
		// TODO: we need some kind of callback on complete to finish this off
		// see FLASStory: 220
//		fn.provideCaseName(caseName);
//		scope.define(errors, fn.functionName().name, fn);
	}

	@Override
	public void functionCase(FunctionCaseDefn fn) {
		int caseName = scope.caseName(fn.intro.name().uniqueName());
		fn.provideCaseName(caseName);
		scope.define(errors, fn.functionName().name, fn);
	}

	@Override
	public void newStruct(StructDefn sd) {
		scope.define(errors, sd.name.baseName(), sd);
	}

	@Override
	public void newContract(ContractDecl decl) {
		scope.define(errors, decl.nameAsName().baseName(), decl);
	}

	@Override
	public void process() {
//		new PFDumper().dumpScope(new Indenter(new PrintWriter(System.out)), scope);
		try {
			// TODO: we would like the sinks to be passed in so that we preserve TDA rather than creating them here 
			CompileResult res = compiler.stage2(errors, null, null, scope.scopeName.uniqueName(), scope);
			bce = res.bce;
			jsFiles = res.jsFiles();
		} catch (ErrorResultException ex) {
			errors.merge(ex.errors);
		} catch (Throwable t) {
			t.printStackTrace();
			errors.message(((InputPosition)null), t.toString());
		}
	}

	@Override
	public void scopeTo(ScopeReceiver sendTo) {
		sendTo.provideScope(scope);
	}

	@Override
	public void bceTo(BCEReceiver sendTo) {
		if (bce == null)
			throw new RuntimeException("Too soon");
		sendTo.provideBCE(bce);
	}

	@Override
	public void jsTo(JSReceiver sendTo) {
		if (jsFiles == null)
			throw new RuntimeException("Too soon");
		sendTo.provideFiles(jsFiles);
	}
}
