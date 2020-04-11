package org.flasck.flas.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.SendMessage;
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
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class Repository implements TopLevelDefinitionConsumer, RepositoryReader {
	public interface Visitor {
		void visitEntry(RepositoryEntry entry);
		void visitPrimitive(Primitive p);
		void visitStructDefn(StructDefn s);
		void visitStructField(StructField sf);
		void leaveStructField(StructField sf);
		void leaveStructDefn(StructDefn s);
		void visitStructFieldAccessor(StructField sf);
		void leaveStructFieldAccessor(StructField sf);
		void visitUnionTypeDefn(UnionTypeDefn ud);
		void leaveUnionTypeDefn(UnionTypeDefn ud);
		void visitUnresolvedVar(UnresolvedVar var, int nargs);
		void visitUnresolvedOperator(UnresolvedOperator operator, int nargs);
		void visitAnonymousVar(AnonymousVar var);
		void visitIntroduceVar(IntroduceVar var);
		void visitTypeReference(TypeReference var);
		void visitFunctionGroup(FunctionGroup grp);
		void visitFunction(FunctionDefinition fn);
		void visitFunctionIntro(FunctionIntro fi);
		void leaveFunctionIntro(FunctionIntro fi);
		void leaveFunction(FunctionDefinition fn);
		void leaveFunctionGroup(FunctionGroup grp);
		void visitTuple(TupleAssignment e);
		void tupleExprComplete(TupleAssignment e);
		void visitTupleMember(TupleMember sd);
		void leaveTupleMember(TupleMember sd);
		void leaveTuple(TupleAssignment e);
		void visitPattern(Pattern patt, boolean isNested);
		void visitVarPattern(VarPattern p, boolean isNested);
		void visitTypedPattern(TypedPattern p, boolean isNested);
		void visitConstructorMatch(ConstructorMatch p, boolean isNested);
		void visitConstructorField(String field, Pattern patt, boolean isNested);
		void leaveConstructorField(String field, Object patt);
		void leaveConstructorMatch(ConstructorMatch p);
		void visitPatternVar(InputPosition varLoc, String var);
		void leavePattern(Object patt, boolean isNested);
		void startInline(FunctionIntro fi);
		void visitCase(FunctionCaseDefn c);
		void visitGuard(FunctionCaseDefn c);
		void leaveGuard(FunctionCaseDefn c);
		void leaveCase(FunctionCaseDefn c);
		void endInline(FunctionIntro fi);
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
		void leaveContractMethod(ContractMethodDecl cmd);
		void leaveContractDecl(ContractDecl cd);
		void visitObjectDefn(ObjectDefn obj);
		void leaveObjectDefn(ObjectDefn obj);
		void visitAgentDefn(AgentDefinition s);
		void visitProvides(Provides p);
		void leaveProvides(Provides p);
		void visitRequires(RequiresContract rc);
		void visitImplements(ImplementsContract ic);
		void leaveImplements(ImplementsContract ic);
		void visitHandlerImplements(HandlerImplements hi);
		void visitHandlerLambda(Pattern p);
		void leaveHandlerImplements(HandlerImplements hi);
		void leaveAgentDefn(AgentDefinition s);
		void visitObjectAccessor(ObjectAccessor oa);
		void leaveObjectAccessor(ObjectAccessor oa);
		void visitStandaloneMethod(StandaloneMethod meth);
		void visitObjectMethod(ObjectMethod meth);
		void visitMessages(Messages messages);
		void visitMessage(ActionMessage msg);
		void visitAssignMessage(AssignMessage msg);
		void visitAssignSlot(List<UnresolvedVar> slot);
		void leaveAssignMessage(AssignMessage msg);
		void visitSendMessage(SendMessage msg);
		void leaveSendMessage(SendMessage msg);
		void leaveMessage(ActionMessage msg);
		void leaveMessages(Messages msgs);
		void leaveObjectMethod(ObjectMethod meth);
		void leaveStandaloneMethod(StandaloneMethod meth);
		void visitMakeSend(MakeSend expr);
		void leaveMakeSend(MakeSend expr);
		void visitMakeAcor(MakeAcor expr);
		void leaveMakeAcor(MakeAcor expr);
		void visitCurrentContainer(CurrentContainer expr);
		void visitAssertExpr(boolean isValue, Expr e);
		void leaveAssertExpr(boolean isValue, Expr e);
		void visitConstPattern(ConstPattern p, boolean isNested);
		void visitMemberExpr(MemberExpr expr);
		void leaveMemberExpr(MemberExpr expr);
		void visitUnitDataDeclaration(UnitDataDeclaration udd);
		void leaveUnitDataDeclaration(UnitDataDeclaration udd);
		void visitUnitDataField(Assignment assign);
		void leaveUnitDataField(Assignment assign);
		void visitUnitTestInvoke(UnitTestInvoke uti);
		void leaveUnitTestInvoke(UnitTestInvoke uti);
		void visitUnitTestExpect(UnitTestExpect s);
		void leaveUnitTestExpect(UnitTestExpect ute);
		void visitUnitTestSend(UnitTestSend s);
		void leaveUnitTestSend(UnitTestSend s);
		void visitSendMethod(NamedType defn, UnresolvedVar expr);
		void visitHandleExpr(InputPosition location, Expr expr, Expr handler);
		void leaveHandleExpr(Expr expr, Expr handler);
		void traversalDone();
	}

	final Map<String, RepositoryEntry> dict = new TreeMap<>();
	
	public Repository() {
	}
	
	@Override
	public void functionDefn(FunctionDefinition func) {
		addEntry(func.name(), func);
	}

	@Override
	public void tupleDefn(List<LocatedName> vars, FunctionName exprFnName, FunctionName pkgName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, pkgName, expr);
		addEntry(exprFnName, ta);
		NameOfThing pkg = pkgName.inContext;
		int k=0;
		for (LocatedName x : vars) {
			FunctionName tn = FunctionName.function(x.location, pkg, x.text);
			TupleMember tm = new TupleMember(x.location, ta, k++, tn);
			addEntry(tn, tm);
			ta.addMember(tm);
		}
	}

	@Override
	public void argument(VarPattern parm) {
		addEntry(parm.name(), parm);
	}

	@Override
	public void argument(TypedPattern parm) {
		addEntry(parm.name(), parm);
	}

	@Override
	public void newHandler(HandlerImplements hi) {
		addEntry(hi.handlerName, hi);
	}

	@Override
	public void newAgent(AgentDefinition decl) {
		addEntry(decl.cardName(), decl);
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
	public void newRequiredContract(RequiresContract rc) {
		addEntry(rc.varName(), rc);
	}
	
	@Override
	public void newStruct(StructDefn sd) {
		sd.completePolyNames();
		addEntry(sd.name(), sd);
		for (PolyType p : sd.polys())
			addEntry(p.name(), p);
	}

	@Override
	public void newStructField(StructField sf) {
		addEntry(sf.name(), sf);
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
		od.completePolyNames();
		addEntry(od.name(), od);
		for (PolyType p : od.polys())
			addEntry(p.name(), p);
	}

	@Override
	public void newObjectAccessor(ObjectAccessor oa) {
		addEntry(oa.name(), oa);
	}

	public void unitTestPackage(UnitTestPackage pkg) {
		addEntry(pkg.name(), pkg);
	}

	@Override
	public void newTestData(UnitDataDeclaration data) {
		addEntry(data.name(), data);
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
	public Type findUnionWith(Set<Type> ms) {
		if (ms.isEmpty())
			throw new NotImplementedException();
		Set<Type> collect = new HashSet<Type>();
		for (Type t : ms) {
			if (t.equals(LoadBuiltins.any))
				return t;
			collect.add(t);
		}
		if (collect.isEmpty())
			return LoadBuiltins.any;
		else if (collect.size() == 1)
			return collect.iterator().next();
		for (RepositoryEntry k : dict.values()) {
			if (k instanceof UnionTypeDefn) {
				UnionTypeDefn utd = (UnionTypeDefn) k;
				Type union = utd.matches(ms);
				if (union != null)
					return union;
			}
		}
		return null;
	}

	@Override
	public void traverse(Visitor visitor) {
		Traverser t = new Traverser(visitor);
		t.doTraversal(this);
	}

	public void traverseWithImplementedMethods(Visitor visitor) {
 		Traverser t = new Traverser(visitor);
 		t.withImplementedMethods();
		t.doTraversal(this);
	}

	public void traverseLifted(Visitor visitor) {
 		Traverser t = new Traverser(visitor);
		t.withNestedPatterns();
		t.doTraversal(this);
	}

	@Override
	public void traverseInGroups(Visitor visitor, FunctionGroups groups) {
 		Traverser t = new Traverser(visitor);
		t.withNestedPatterns();
		t.withFunctionsInDependencyGroups(groups);
		t.withPatternsInTreeOrder();
		t.doTraversal(this);
	}

	@Override
	public void traverseWithMemberFields(Visitor visitor) {
 		Traverser t = new Traverser(visitor);
		t.withMemberFields();
		t.doTraversal(this);
	}

	@Override
	public void traverseWithHSI(HSIVisitor v) {
		Traverser t = new Traverser(v).withHSI().withNestedPatterns();
		t.doTraversal(this);
	}

	@Override
	public void dump() {
		for (RepositoryEntry e : dict.values())
			System.out.println(e.name().uniqueName() + " => " + e);
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
