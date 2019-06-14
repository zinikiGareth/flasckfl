package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

public class Repository implements TopLevelDefinitionConsumer {
	class FunctionBits {
		final FunctionIntro intro;
		final List<FunctionCaseDefn> defns = new ArrayList<>();

		public FunctionBits(FunctionIntro fn) {
			this.intro = fn;
		}
		
		public FunctionName caseName() {
			int cs = defns.size();
			return FunctionName.caseName(intro.name(), cs);
		}

		public void dumpTo(PrintWriter pw) {
			pw.println(this.intro);
			for (FunctionCaseDefn fcd : defns) {
				pw.print("  ");
				pw.println(fcd);
			}
		}
	}
	private final Map<String, FunctionBits> functions = new TreeMap<>();
	
	public Repository() {
	}
	
	@Override
	public void functionIntro(FunctionIntro fn) {
		final FunctionName fnName = fn.name();
		final String name = fnName.uniqueName();
		if (functions.containsKey(name))
			throw new DuplicateNameException(fnName);
		FunctionBits bits = new FunctionBits(fn);
		functions.put(name, bits);
		// TODO: we need some kind of callback on complete to finish this off
		// see FLASStory: 220
//		fn.provideCaseName(bits.caseName());
//		scope.define(errors, fn.functionName().name, fn);
	}

	@Override
	public void functionCase(FunctionCaseDefn fn) {
		final FunctionName fnName = fn.intro.name();
		final String name = fnName.uniqueName();
		FunctionBits bits;
		if (!functions.containsKey(name)) {
			bits = new FunctionBits(fn.intro);
			functions.put(name, bits);
		} else {
			bits = functions.get(name);
		}
		bits.defns.add(fn);
		fn.provideCaseName(bits.caseName());
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

	public void dumpTo(File dumpRepo) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(dumpRepo);
		for (Entry<String, FunctionBits> x : functions.entrySet()) {
			pw.print(x.getKey() + " = ");
			x.getValue().dumpTo(pw);
		}
		pw.close();
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
