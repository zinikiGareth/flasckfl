package org.flasck.flas.compiler;

import java.io.File;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
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

	@Override
	public CardName cardName(String name) {
		return new CardName((PackageName) scope.scopeName, name);
	}
	
	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, scope.name(), base);
	}
	
	@Override
	public void functionIntro(FunctionIntro fn) {
//		int caseName = scope.caseName(fn.name().uniqueName());
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
	public void tupleDefn(List<LocatedName> vars, FunctionName exprFnName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, expr);
		int k=0;
		for (LocatedName x : vars) {
			scope.define(errors, x.text, new TupleMember(x.location, ta, k++, functionName(x.location, x.text)));
		}
	}

	@Override
	public void newCard(CardDefinition decl) {
		scope.define(errors, decl.simpleName, decl);
	}

	@Override
	public void newService(ServiceDefinition card) {
		throw new org.zinutils.exceptions.NotImplementedException();
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
	public void newObject(ObjectDefn od) {
		scope.define(errors, od.name().baseName(), od);
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
