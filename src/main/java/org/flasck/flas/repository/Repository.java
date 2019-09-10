package org.flasck.flas.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.tc3.Primitive;

public class Repository implements TopLevelDefinitionConsumer, RepositoryReader {
	public interface Visitor {
		void visitEntry(RepositoryEntry entry);
		void visitPrimitive(Primitive p);
		void visitStructDefn(StructDefn s);
		void visitStructField(StructField sf);
		void leaveStructDefn(StructDefn s);
		void visitUnresolvedVar(UnresolvedVar var, int nargs);
		void visitUnresolvedOperator(UnresolvedOperator operator, int nargs);
		void visitTypeReference(TypeReference var);
		void visitFunction(FunctionDefinition fn);
		void leaveFunction(FunctionDefinition fn);
		void visitIntro(FunctionIntro fi);
		void visitPattern(Object patt);
		void visitVarPattern(VarPattern p);
		void visitTypedPattern(TypedPattern p);
		void visitPatternVar(InputPosition varLoc, String var);
		void visitCase(FunctionCaseDefn c);
		void visitExpr(Expr expr, int nArgs);
		void visitStringLiteral(StringLiteral expr);
		void visitNumericLiteral(NumericLiteral number);
		void visitUnitTestPackage(UnitTestPackage e);
		void visitUnitTest(UnitTestCase e);
		void leaveUnitTest(UnitTestCase e);
		void leaveUnitTestPackage(UnitTestPackage e);
		void visitApplyExpr(ApplyExpr expr);
		void leaveApplyExpr(ApplyExpr expr);
		void visitUnitTestStep(UnitTestStep s);
		void visitUnitTestAssert(UnitTestAssert a);
		void postUnitTestAssert(UnitTestAssert a);
		void visitContractDecl(ContractDecl cd);
		void visitContractMethod(ContractMethodDecl cmd);
		void leaveContractDecl(ContractDecl cd);
		void visitObjectDefn(ObjectDefn e);
		void visitObjectMethod(ObjectMethod e);
	}

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
	public void newUnion(UnionTypeDefn ud) {
		addEntry(ud.name(), ud);
	}

	@Override
	public void newContract(ContractDecl decl) {
		addEntry(decl.name(), decl);
	}

	@Override
	public void newObject(ObjectDefn od) {
		addEntry(od.name(), od);
	}

	public void unitTestPackage(UnitTestPackage pkg) {
		addEntry(pkg.name(), pkg);
	}

	public void addEntry(final NameOfThing name, final RepositoryEntry entry) {
		if (dict.containsKey(name.uniqueName()))
			throw new DuplicateNameException(name);
		dict.put(name.uniqueName(), entry);
	}

	public void dumpTo(File dumpRepo) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(dumpRepo);
		dumpTo(pw);
		pw.close();
	}

	public void dumpTo(PrintWriter pw) {
		for (Entry<String, RepositoryEntry> x : dict.entrySet()) {
			pw.print(x.getKey() + " = ");
			x.getValue().dumpTo(pw);
		}
		pw.flush();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends RepositoryEntry> T get(String string) {
		return (T)dict.get(string);
	}

	@Override
	public void traverse(Visitor visitor) {
		Traverser t = new Traverser(visitor);
		for (RepositoryEntry e : dict.values()) {
			t.visitEntry(e);
		}
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
