package org.flasck.flas.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.compiler.StateNameException;
import org.flasck.flas.errors.ErrorReporter;
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
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TemplateStylingOption;
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
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.SplitMetaData;
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
		void visitHandlerImplements(HandlerImplements hi, StateHolder sh);
		void visitHandlerLambda(HandlerLambda hl);
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
		void expectHandlerNext();
		void leaveUnitTestExpect(UnitTestExpect ute);
		void visitUnitTestSend(UnitTestSend s);
		void leaveUnitTestSend(UnitTestSend s);
		void visitUnitTestEvent(UnitTestEvent e);
		void leaveUnitTestEvent(UnitTestEvent e);
		void visitSendMethod(NamedType defn, UnresolvedVar expr);
		void visitHandleExpr(InputPosition location, Expr expr, Expr handler);
		void leaveHandleExpr(Expr expr, Expr handler);
		void traversalDone();
		void visitObjectContract(ObjectContract oc);
		void leaveObjectContract(ObjectContract oc);
		void visitObjectCtor(ObjectCtor oa);
		void leaveObjectCtor(ObjectCtor oa);
		void leaveStateDefinition(StateDefinition state);
		void visitStateDefinition(StateDefinition state);
		void visitServiceDefn(ServiceDefinition s);
		void leaveServiceDefn(ServiceDefinition s);
		void visitCardDefn(CardDefinition cd);
		void leaveCardDefn(CardDefinition s);
		void visitTemplate(Template t, boolean isFirst);
		void leaveTemplate(Template t);
		void visitUnitTestMatch(UnitTestMatch m);
		void leaveUnitTestMatch(UnitTestMatch m);
		void visitTemplateReference(TemplateReference refersTo, boolean isFirst);
		void visitTemplateBinding(TemplateBinding b);
		void visitTemplateBindingOption(TemplateBindingOption option);
		void leaveTemplateBindingOption(TemplateBindingOption option);
		void leaveTemplateBinding(TemplateBinding b);
		void visitTemplateCustomization(TemplateCustomization tc);
		void leaveTemplateCustomization(TemplateCustomization tc);
		void visitTemplateStyling(TemplateStylingOption tso);
		void leaveTemplateStyling(TemplateStylingOption tso);
		void visitTemplateEvent(TemplateEvent te);
		void leaveTemplateEvent(TemplateEvent te);
	}

	final Map<String, RepositoryEntry> dict = new TreeMap<>();
	private final List<SplitMetaData> webs = new ArrayList<>();
	
	public Repository() {
	}
	
	@Override
	public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
		addEntry(errors, func.name(), func);
	}

	@Override
	public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName exprFnName, FunctionName pkgName, Expr expr) {
		TupleAssignment ta = new TupleAssignment(vars, exprFnName, pkgName, expr);
		NameOfThing pkg = pkgName.inContext;
		int k=0;
		for (LocatedName x : vars) {
			FunctionName tn = FunctionName.function(x.location, pkg, x.text);
			TupleMember tm = new TupleMember(x.location, ta, k++, tn);
			addEntry(errors, tn, tm);
			ta.addMember(tm);
		}
		try {
			addEntry(null, exprFnName, ta);
		} catch (DuplicateNameException | StateNameException ex) {
			// if this is thrown, it is because vars[0] is a duplicate
			// that (should) have already flagged an error above
		}
	}

	@Override
	public void argument(ErrorReporter errors, VarPattern parm) {
		addEntry(errors, parm.name(), parm);
	}

	@Override
	public void argument(ErrorReporter errors, TypedPattern parm) {
		addEntry(errors, parm.name(), parm);
	}

	@Override
	public void newHandler(ErrorReporter errors, HandlerImplements hi) {
		addEntry(errors, hi.handlerName, hi);
	}

	@Override
	public void newAgent(ErrorReporter errors, AgentDefinition decl) {
		addEntry(errors, decl.cardName(), decl);
	}

	@Override
	public void newCard(ErrorReporter errors, CardDefinition decl) {
		addEntry(errors, decl.cardName(), decl);
	}

	@Override
	public void newService(ErrorReporter errors, ServiceDefinition svc) {
		addEntry(errors, svc.cardName(), svc);
	}

	@Override
	public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
		addEntry(errors, meth.name(), meth);
	}

	@Override
	public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
		addEntry(errors, om.name(), om);
	}

	@Override
	public void newRequiredContract(ErrorReporter errors, RequiresContract rc) {
		addEntry(errors, rc.varName(), rc);
	}
	
	@Override
	public void newObjectContract(ErrorReporter errors, ObjectContract oc) {
		addEntry(errors, oc.varName(), oc);
	}
	
	@Override
	public void newStruct(ErrorReporter errors, StructDefn sd) {
		sd.completePolyNames();
		addEntry(errors, sd.name(), sd);
		for (PolyType p : sd.polys())
			addEntry(errors, p.name(), p);
	}

	@Override
	public void newStructField(ErrorReporter errors, StructField sf) {
		addEntry(errors, sf.name(), sf);
	}

	@Override
	public void newUnion(ErrorReporter errors, UnionTypeDefn ud) {
		addEntry(errors, ud.name(), ud);
	}

	@Override
	public void newContract(ErrorReporter errors, ContractDecl decl) {
		addEntry(errors, decl.name(), decl);
	}

	@Override
	public void newObject(ErrorReporter errors, ObjectDefn od) {
		od.completePolyNames();
		addEntry(errors, od.name(), od);
		for (PolyType p : od.polys())
			addEntry(errors, p.name(), p);
	}

	@Override
	public void newObjectAccessor(ErrorReporter errors, ObjectAccessor oa) {
		addEntry(errors, oa.name(), oa);
	}

	public void unitTestPackage(ErrorReporter errors, UnitTestPackage pkg) {
		addEntry(errors, pkg.name(), pkg);
	}

	@Override
	public void newTestData(ErrorReporter errors, UnitDataDeclaration data) {
		addEntry(errors, data.name(), data);
	}

	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		addEntry(errors, var.name(), var);
	}

	public void addEntry(ErrorReporter errors, final NameOfThing name, final RepositoryEntry entry) {
		String un = name.uniqueName();
		if (!checkNoStateConflicts(errors, name, entry)) {
			return;
		}
		if (dict.containsKey(un)) {
			if (errors != null) {
				errors.message(entry.location(), un + " is defined multiple times: " + dict.get(un).location());
				return;
			} else
				throw new DuplicateNameException(name);
		}
		dict.put(un, entry);
	}

	private boolean checkNoStateConflicts(ErrorReporter errors, NameOfThing name, RepositoryEntry entry) {
		if (entry instanceof StructField) {
			VarName vn = (VarName) name;
			String cname = vn.container().uniqueName() + ".";
			boolean conflicts = false;
			for (Entry<String, RepositoryEntry> e : dict.entrySet()) {
				if (e.getKey().startsWith(cname)) {
					NameOfThing rn = e.getValue().name();
					String base;
					if (rn instanceof FunctionName)
						base = ((FunctionName)rn).name;
					else if (rn instanceof VarName)
						base = ((VarName)rn).var;
					else
						continue;
					if (base.equals(vn.var)) {
						if (errors == null)
							throw new NotImplementedException("we should be passed errors in this case - figure it out");
						else
							errors.message(e.getValue().location(), "cannot use " + base + " here as it conflicts with state member at " + vn.loc);
						conflicts = true;
					}
				}
			}
			return !conflicts;
		} else if (name instanceof FunctionName || name instanceof VarName) {
			String base;
			if (name instanceof FunctionName)
				base = ((FunctionName)name).name;
			else if (name instanceof VarName)
				base = ((VarName)name).var;
			else
				throw new NotImplementedException("cannot extract base from " + name);
			NameOfThing n1 = name;
			while (n1 != null && !(n1 instanceof PackageName)) {
				if (n1 instanceof SolidName || n1 instanceof CardName) {
					RepositoryEntry other = dict.get(n1.uniqueName());
					if (other instanceof StateHolder) {
						StateDefinition state = ((StateHolder)other).state();
						if (state != null && state.hasMember(base)) {
							if (errors == null)
								throw new StateNameException(state.findField(base).name());
							else
								errors.message(entry.location(), "cannot use " + base + " here as it conflicts with state member at " + state.findField(base).loc);
							return false;
						}
					}
					break;
				}
				n1 = n1.container();
			}
			return true;
		} else
			return true;
	}

	@Override
	public void replaceDefinition(HandlerLambda hl) {
		if (!dict.containsKey(hl.name().uniqueName()))
			throw new NotImplementedException(hl.name().uniqueName() + " was not defined");
		dict.put(hl.name().uniqueName(), hl);
	}

	public void webData(SplitMetaData md) {
		webs.add(md);
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

	public void traverseAssemblies(AssemblyVisitor v) {
		AssemblyTraverser t = new AssemblyTraverser(v);
		t.doTraversal(this);
	}

	@Override
	public CardData findWeb(String baseName) {
		for (SplitMetaData web : webs) {
			CardData ret = web.forCard(baseName);
			if (ret != null)
				return ret;
		}
		return null;
	}

	public Iterable<SplitMetaData> allWebs() {
		return webs;
	}

	@Override
	public void dump() {
		for (RepositoryEntry e : dict.values())
			System.out.println(e.name().uniqueName() + " => " + e);
	}
}
