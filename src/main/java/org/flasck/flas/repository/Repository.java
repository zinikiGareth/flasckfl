package org.flasck.flas.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public class Repository implements TopLevelDefinitionConsumer {
	private final Map<String, RepositoryEntry> dict = new TreeMap<>();
	
	public Repository() {
	}
	
	@Override
	public void functionDefn(FunctionDefinition func) {
		addEntry(func.name(), func);
	}

	@Override
	public void tupleDefn(List<LocatedName> vars, FunctionName exprFnName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, expr);
		addEntry(exprFnName, ta);
		NameOfThing pkg = exprFnName.inContext;
		int k=0;
		for (LocatedName x : vars) {
			FunctionName tn = FunctionName.function(x.location, pkg, x.text);
			addEntry(tn, new TupleMember(x.location, ta, k++, tn));
		}
	}

	@Override
	public void argument(VarPattern parm) {
		addEntry(parm.name(), parm);
	}

	@Override
	public void newHandler(HandlerImplements hi) {
		addEntry(hi.handlerName, hi);
	}

	@Override
	public void newCard(CardDefinition decl) {
		addEntry(decl.cardName(), decl);
	}

	@Override
	public void newService(ServiceDefinition svc) {
		addEntry(svc.cardName(), svc);
	}

	@Override
	public void newStandaloneMethod(StandaloneMethod meth) {
		addEntry(meth.name(), meth);
	}

	@Override
	public void newObjectMethod(ObjectActionHandler om) {
		addEntry(om.name(), om);
	}

	@Override
	public void newStruct(StructDefn sd) {
		addEntry(sd.name(), sd);
	}

	@Override
	public void field(NameOfThing name, StructField sf) {
		addEntry(name, sf);
	}

	@Override
	public void newUnion(UnionTypeDefn ud) {
		addEntry(ud.myName(), ud);
	}

	@Override
	public void newContract(ContractDecl decl) {
		addEntry(decl.nameAsName(), decl);
	}

	@Override
	public void newObject(ObjectDefn od) {
		addEntry(od.name(), od);
	}

	public void addEntry(final NameOfThing name, final RepositoryEntry entry) {
		if (dict.containsKey(name.uniqueName()))
			throw new DuplicateNameException(name);
		dict.put(name.uniqueName(), entry);
	}

	public void dumpTo(File dumpRepo) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(dumpRepo);
		for (Entry<String, RepositoryEntry> x : dict.entrySet()) {
			pw.print(x.getKey() + " = ");
			x.getValue().dumpTo(pw);
		}
		pw.close();
	}

	@SuppressWarnings("unchecked")
	public <T extends RepositoryEntry> T get(String string) {
		return (T)dict.get(string);
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
