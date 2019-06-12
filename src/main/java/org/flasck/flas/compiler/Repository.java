package org.flasck.flas.compiler;

import java.io.File;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class Repository implements TopLevelDefinitionConsumer {
	private ByteCodeEnvironment bce;
	private Iterable<File> jsFiles;
	
	public Repository() {
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
//		int caseName = scope.caseName(fn.intro.name().uniqueName());
//		fn.provideCaseName(caseName);
	}

	@Override
	public void tupleDefn(List<LocatedName> vars, FunctionName exprFnName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, expr);
		int k=0;
//		for (LocatedName x : vars) {
//			scope.define(errors, x.text, new TupleMember(x.location, ta, k++, functionName(x.location, x.text)));
//		}
	}

	@Override
	public void newHandler(HandlerImplements hi) {
	}

	@Override
	public void newCard(CardDefinition decl) {
//		scope.define(errors, decl.simpleName, decl);
	}

	@Override
	public void newService(ServiceDefinition card) {
	}

	@Override
	public void newStandaloneMethod(ObjectMethod meth) {
	}

	@Override
	public void newStruct(StructDefn sd) {
//		scope.define(errors, sd.name.baseName(), sd);
	}

	@Override
	public void newUnion(UnionTypeDefn with) {
	}

	@Override
	public void newContract(ContractDecl decl) {
//		scope.define(errors, decl.nameAsName().baseName(), decl);
	}

	@Override
	public void newObject(ObjectDefn od) {
//		scope.define(errors, od.name().baseName(), od);
	}

//	@Override
//	public void process() {
//		new PFDumper().dumpScope(new Indenter(new PrintWriter(System.out)), scope);
//		try {
//			// TODO: we would like the sinks to be passed in so that we preserve TDA rather than creating them here 
//			CompileResult res = compiler.stage2(errors, null, null, scope.scopeName.uniqueName(), scope);
//			bce = res.bce;
//			jsFiles = res.jsFiles();
//		} catch (ErrorResultException ex) {
//			errors.merge(ex.errors);
//		} catch (Throwable t) {
//			t.printStackTrace();
//			errors.message(((InputPosition)null), t.toString());
//		}
//	}

//	@Override
//	public void bceTo(BCEReceiver sendTo) {
//		if (bce == null)
//			throw new RuntimeException("Too soon");
//		sendTo.provideBCE(bce);
//	}
//
//	@Override
//	public void jsTo(JSReceiver sendTo) {
//		if (jsFiles == null)
//			throw new RuntimeException("Too soon");
//		sendTo.provideFiles(jsFiles);
//	}
}
