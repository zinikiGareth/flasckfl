package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.DeferMeException;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerHolder;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PatternsHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneDefn;
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
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestNewDiv;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.patterns.HSICtorTree;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIOptions.IntroTypeVar;
import org.flasck.flas.patterns.HSIOptions.IntroVarName;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements RepositoryVisitor {
	private final static Logger hsiLogger = LoggerFactory.getLogger("HSI");
	private final RepositoryVisitor visitor;
	private StandaloneDefn currentFunction;
	private FunctionGroups functionOrder;
	private boolean wantImplementedMethods = false;
	private boolean wantNestedPatterns;
	private boolean wantHSI;
	private boolean wantEventSources = false;
	private boolean patternsTree;
	private boolean visitMemberFields = false;
	private boolean isConverted;

	public Traverser(RepositoryVisitor visitor) {
		this.visitor = visitor;
	}

	public Traverser withImplementedMethods() {
		this.wantImplementedMethods = true;
		return this;
	}

	public Traverser withNestedPatterns() {
		this.wantNestedPatterns = true;
		return this;
	}

	public Traverser withHSI() {
		this.wantHSI = true;
		return this;
	}

	public Traverser withFunctionsInDependencyGroups(FunctionGroups order) {
		this.functionOrder = order;
		return this;
	}

	public Traverser withPatternsInTreeOrder() {
		this.patternsTree = true;
		return this;
	}

	public Traverser withMemberFields() {
		this.visitMemberFields = true;
		return this;
	}

	public Traverser withEventSources() {
		this.wantEventSources = true;
		return this;
	}

	public void doTraversal(Repository repository) {
		if (functionOrder != null) {
			Iterator<FunctionGroup> todo = functionOrder.iterator();
			int cnt = functionOrder.size();
			while (todo.hasNext()) {
				List<FunctionGroup> undone = new ArrayList<>();
				while (todo.hasNext()) {
					FunctionGroup grp = todo.next();
					try {
						visitFunctionGroup(grp);
					} catch (DeferMeException ex) {
						((StackVisitor)visitor).reduceTo(1);
						System.out.println("deferring " + grp + " " + visitor);
						undone.add(grp);
					}
				}
				if (undone.size() == cnt)
					throw new HaventConsideredThisException("There appears to be no order in which these functions can be processed: " + undone);
				todo = undone.iterator();
				cnt = undone.size();
			}
		}
		Set<RepositoryEntry> entriesInSomeOrder = new TreeSet<RepositoryEntry>(RepositoryEntry.preferredOrder);
		entriesInSomeOrder.addAll(repository.dict.values());
		for (RepositoryEntry e : entriesInSomeOrder) {
			visitEntry(e);
		}
		traversalDone();
	}

	/** It's starting to concern me that for some things (contracts, unit tests) we visit
	 * the parent object and then all of its children, but for other things,
	 * such as objects and their methods, we view both as being in the repository and allow
	 * the repository to do its work in any order.
	 */
	@Override
	public void visitEntry(RepositoryEntry e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null entries");
		else if (e instanceof Primitive)
			visitPrimitive((Primitive)e);
		else if (e instanceof ContractDecl)
			visitContractDecl((ContractDecl)e);
		else if (e instanceof ObjectDefn)
			visitObjectDefn((ObjectDefn)e);
		else if (e instanceof AgentDefinition)
			visitAgentDefn((AgentDefinition)e);
		else if (e instanceof ServiceDefinition)
			visitServiceDefn((ServiceDefinition)e);
		else if (e instanceof CardDefinition)
			visitCardDefn((CardDefinition)e);
		else if (e instanceof FunctionDefinition) {
			if (functionOrder == null)
				visitFunction((FunctionDefinition)e);
		} else if (e instanceof TupleAssignment) {
			// Do we need to think about function ordering?
			visitTuple((TupleAssignment)e);
		} else if (e instanceof TupleMember) {
			// if needed, it should be visited within the assignment
		} else if (e instanceof ObjectMethod) {
			visitObjectMethod((ObjectMethod)e);
		} else if (e instanceof ObjectAccessor) {
			if (functionOrder == null)
				visitObjectAccessor((ObjectAccessor)e);
		} else if (e instanceof ObjectCtor) {
			visitObjectCtor((ObjectCtor)e);
		} else if (e instanceof StandaloneMethod) {
			if (functionOrder == null)
				visitStandaloneMethod((StandaloneMethod)e);
		} else if (e instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) e;
			if (hi.getParent() == null)
				visitHandlerImplements(hi, null);
		} else if (e instanceof StructDefn)
			visitStructDefn((StructDefn)e);
		else if (e instanceof UnionTypeDefn)
			visitUnionTypeDefn((UnionTypeDefn)e);
		else if (e instanceof UnitTestPackage)
			visitUnitTestPackage((UnitTestPackage)e);
		else if (e instanceof UnitDataDeclaration) {
			; // even top level ones are in a package ...
//			UnitDataDeclaration udd = (UnitDataDeclaration) e;
//			if (udd.isTopLevel())
//				visitUnitDataDeclaration(udd);
		} else if (e instanceof StructField) {
			visitStructFieldAccessor((StructField) e);
		} else if (e instanceof VarPattern || e instanceof TypedPattern || e instanceof IntroduceVar || e instanceof HandlerLambda ||
				   e instanceof PolyType || e instanceof RequiresContract || e instanceof ObjectContract ||
				   e instanceof Template) {
			; // do nothing: these are just in the repo for lookup purposes
		} else if (e instanceof Assembly) {
			;
		} else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle " + e.getClass());
	}

	@Override
	public void visitPrimitive(Primitive p) {
		visitor.visitPrimitive(p);
	}

	@Override
	public void visitStructDefn(StructDefn s) {
		visitor.visitStructDefn(s);
		for (StructField f : s.fields)
			visitStructField(f);
		leaveStructDefn(s);
	}
	
	@Override
	public void visitStructField(StructField sf) {
		visitor.visitStructField(sf);
		visitTypeReference(sf.type, true);
		if (sf.init != null)
			visitExpr(sf.init, 0);
		leaveStructField(sf);
	}

	public void leaveStructField(StructField sf) {
		visitor.leaveStructField(sf);
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
		visitor.leaveStructDefn(s);
	}

	@Override
	public void visitStructFieldAccessor(StructField sf) {
		if (wantHSI && sf.accessor) {
			visitor.visitStructFieldAccessor(sf);
			leaveStructFieldAccessor(sf);
		}
	}
	
	@Override
	public void leaveStructFieldAccessor(StructField sf) {
		visitor.leaveStructFieldAccessor(sf);
	}
	
	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		visitor.visitUnionTypeDefn(ud);
		for (TypeReference c : ud.cases)
			visitTypeReference(c, true);
		leaveUnionTypeDefn(ud);
	}
	
	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		visitor.leaveUnionTypeDefn(ud);
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		visitor.visitObjectDefn(obj);
		visitStateDefinition(obj.state());
		for (ObjectContract oc : obj.contracts)
			visitObjectContract(oc);
		for (HandlerImplements ic : obj.handlers)
			visitHandlerImplements(ic, obj);
		for (Template t : obj.templates) {
			visitTemplate(t, false);
		}
		leaveObjectDefn(obj);
	}

	@Override
	public void visitStateDefinition(StateDefinition state) {
		if (state != null) {
			visitor.visitStateDefinition(state);
			for (StructField f : state.fields)
				visitStructField(f);
			leaveStateDefinition(state);
		}
	}

	@Override
	public void leaveStateDefinition(StateDefinition state) {
		visitor.leaveStateDefinition(state);
	}

	@Override
	public void visitObjectContract(ObjectContract oc) {
		visitor.visitObjectContract(oc);
		visitTypeReference(oc.implementsType(), true);
		leaveObjectContract(oc);
	}

	@Override
	public void leaveObjectContract(ObjectContract oc) {
		visitor.leaveObjectContract(oc);
	}

	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		visitor.leaveObjectDefn(obj);
	}

	@Override
	public void visitCardDefn(CardDefinition cd) {
		visitor.visitCardDefn(cd);
		visitStateDefinition(cd.state());
		for (RequiresContract rc : cd.requires)
			visitRequires(rc);
		for (Provides p : cd.services)
			visitProvides(p);
		for (ImplementsContract ic : cd.contracts)
			visitImplements(ic);
		for (HandlerImplements ic : cd.handlers)
			visitHandlerImplements(ic, cd);
		boolean isFirst = true;
		for (Template t : cd.templates) {
			visitTemplate(t, isFirst);
			isFirst = false;
		}
		leaveCardDefn(cd);
	}

	@Override
	public void visitAgentDefn(AgentDefinition s) {
		visitor.visitAgentDefn(s);
		visitStateDefinition(s.state());
		for (RequiresContract rc : s.requires)
			visitRequires(rc);
		for (Provides p : s.services)
			visitProvides(p);
		for (ImplementsContract ic : s.contracts)
			visitImplements(ic);
		for (HandlerImplements ic : s.handlers)
			visitHandlerImplements(ic, s);
		leaveAgentDefn(s);
	}

	@Override
	public void visitServiceDefn(ServiceDefinition s) {
		visitor.visitServiceDefn(s);
		for (RequiresContract rc : s.requires)
			visitRequires(rc);
		for (Provides p : s.provides)
			visitProvides(p);
		for (HandlerImplements ic : s.handlers)
			visitHandlerImplements(ic, null);
		leaveServiceDefn(s);
	}

	public void visitProvides(Provides p) {
		visitor.visitProvides(p);
		visitTypeReference(p.implementsType(), true);
		if (wantImplementedMethods) {
			for (ObjectMethod om : p.implementationMethods)
				visitObjectMethod(om);
		}
		leaveProvides(p);
	}

	public void leaveProvides(Provides p) {
		visitor.leaveProvides(p);
	}

	
	public void visitRequires(RequiresContract rc) {
		visitor.visitRequires(rc);
		visitTypeReference(rc.implementsType(), true);
	}

	public void visitImplements(ImplementsContract ic) {
		visitor.visitImplements(ic);
		visitTypeReference(ic.implementsType(), true);
		if (wantImplementedMethods) {
			for (ObjectMethod om : ic.implementationMethods)
				visitObjectMethod(om);
		}
		leaveImplements(ic);
	}

	public void leaveImplements(ImplementsContract ic) {
		visitor.leaveImplements(ic);
	}

	public void visitHandlerImplements(HandlerImplements hi, StateHolder sh) {
		visitor.visitHandlerImplements(hi, sh);
		visitTypeReference(hi.implementsType(), true);
		traverseHandlerLambdas(hi);
		if (wantImplementedMethods) {
			for (ObjectMethod om : hi.implementationMethods)
				visitObjectMethod(om);
		}
		leaveHandlerImplements(hi);
	}

	public void leaveHandlerImplements(HandlerImplements hi) {
		visitor.leaveHandlerImplements(hi);
	}

	public void visitTemplate(Template t, boolean isFirst) {
		visitor.visitTemplate(t, isFirst);
		if (t.nestingChain() != null) {
			for (TypeReference ty : t.nestingChain().types())
				visitTypeReference(ty, true);
		}
		afterTemplateChainTypes(t);
		NestingChain chain = t.nestingChain();
		if (chain != null) {
			for (TypeReference ty : chain.types())
				visitTypeReference(ty, true);
		}
		for (TemplateBinding b : t.bindings()) {
			visitTemplateBinding(b);
		}
		for (TemplateStylingOption tso : t.stylings())
			visitTemplateStyling(tso);
		leaveTemplate(t);
	}

	public void visitTemplateReference(TemplateReference refersTo, boolean isFirst, boolean isDefining) {
		visitor.visitTemplateReference(refersTo, isFirst, isDefining);
	}

	public void afterTemplateChainTypes(Template t) {
		visitor.afterTemplateChainTypes(t);
	}

	public void leaveTemplate(Template t) {
		visitor.leaveTemplate(t);
	}

	public void visitTemplateBinding(TemplateBinding b) {
		visitor.visitTemplateBinding(b);
		for (TemplateBindingOption c : b.conditional()) {
			visitTemplateBindingOption(c);
		}
		if (b.defaultBinding != null)
			visitTemplateBindingOption(b.defaultBinding);
		visitTemplateCustomization(b);
		leaveTemplateBinding(b);
	}

	public void visitTemplateBindingOption(TemplateBindingOption option) {
		visitor.visitTemplateBindingOption(option);
		if (option.cond != null) {
			visitTemplateBindingCondition(option.cond);
			visitExpr(option.cond, 0);
		}
		visitTemplateBindingExpr(option.expr);
		visitExpr(option.expr, 0);
		visitTemplateCustomization(option);
		leaveTemplateBindingOption(option);
	}

	public void visitTemplateBindingExpr(Expr expr) {
		visitor.visitTemplateBindingExpr(expr);
	}

	public void visitTemplateBindingCondition(Expr cond) {
		visitor.visitTemplateBindingCondition(cond);
	}

	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		visitor.leaveTemplateBindingOption(option);
	}

	public void leaveTemplateBinding(TemplateBinding b) {
		visitor.leaveTemplateBinding(b);
	}

	public void visitTemplateCustomization(TemplateCustomization tc) {
		visitor.visitTemplateCustomization(tc);
		for (TemplateStylingOption tso : tc.conditionalStylings)
			visitTemplateStyling(tso);
		for (TemplateEvent te : tc.events)
			visitTemplateEvent(te);
		leaveTemplateCustomization(tc);
	}

	public void leaveTemplateCustomization(TemplateCustomization tc) {
		visitor.leaveTemplateCustomization(tc);
	}

	public void visitTemplateStyling(TemplateStylingOption tso) {
		visitor.visitTemplateStyling(tso);
		if (tso.cond != null) {
			visitTemplateStyleCond(tso.cond);
			visitExpr(tso.cond, 0);
		}
		for (Expr e : tso.styles) {
			if (!(e instanceof StringLiteral))
				visitTemplateStyleExpr(e);
		}
		leaveTemplateStyling(tso);
	}

	public void visitTemplateStyleCond(Expr cond) {
		visitor.visitTemplateStyleCond(cond);
	}

	public void visitTemplateStyleExpr(Expr e) {
		visitor.visitTemplateStyleExpr(e);
		visitExpr(e, 0);
	}

	public void leaveTemplateStyling(TemplateStylingOption tso) {
		visitor.leaveTemplateStyling(tso);
	}
	
	public void visitTemplateEvent(TemplateEvent te) {
		visitor.visitTemplateEvent(te);
	}

	@Override
	public void leaveCardDefn(CardDefinition s) {
		visitor.leaveCardDefn(s);
	}

	@Override
	public void leaveAgentDefn(AgentDefinition s) {
		visitor.leaveAgentDefn(s);
	}

	public void leaveServiceDefn(ServiceDefinition s) {
		visitor.leaveServiceDefn(s);
	}

	@Override
	public void visitObjectAccessor(ObjectAccessor oa) {
		visitor.visitObjectAccessor(oa);
		visitFunction(oa.function());
		leaveObjectAccessor(oa);
	}

	@Override
	public void leaveObjectAccessor(ObjectAccessor oa) {
		visitor.leaveObjectAccessor(oa);
	}

	@Override
	public void visitObjectCtor(ObjectCtor oc) {
		visitor.visitObjectCtor(oc);
		visitStateDefinition(oc.getObject().state());
		traverseFnOrMethod(oc);
		leaveObjectCtor(oc);
	}

	@Override
	public void leaveObjectCtor(ObjectCtor oc) {
		visitor.leaveObjectCtor(oc);
	}

	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
		visitor.visitFunctionGroup(grp);
		for (StandaloneDefn sd : grp.functions()) {
			if (sd instanceof FunctionDefinition)
				visitFunction((FunctionDefinition) sd);
			else if (sd instanceof StandaloneMethod)
				visitStandaloneMethod((StandaloneMethod) sd);
			else if (sd instanceof TupleAssignment)
				visitTuple((TupleAssignment) sd);
			else if (sd instanceof TupleMember)
				visitTupleMember((TupleMember) sd);
			else
				throw new NotImplementedException("visit " + sd.getClass());
		}
		leaveFunctionGroup(grp);
	}

	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
		rememberCaller(meth);
		visitor.visitStandaloneMethod(meth);
		visitObjectMethod(meth.om);
		leaveStandaloneMethod(meth);
		rememberCaller(null);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		visitor.visitObjectMethod(meth);
		if (wantEventSources && meth.isEvent()) {
			for (Template e : meth.eventSourceExprs()) {
				visitEventSource(e);
			}
		}
		if (!meth.args().isEmpty() || meth.hasMessages()) {
			if (meth.hasImplements() && meth.getImplements() instanceof HandlerImplements)
				traverseHandlerLambdas((HandlerImplements)meth.getImplements());
			traverseFnOrMethod(meth);
		}
		leaveObjectMethod(meth);
	}

	@Override
	public void visitEventSource(Template t) {
		visitor.visitEventSource(t);
	}

	private void traverseHandlerLambdas(HandlerImplements hi) {
		for (HandlerLambda i : hi.boundVars)
			visitHandlerLambda(i);
	}

	public void visitHandlerLambda(HandlerLambda i) {
		visitor.visitHandlerLambda(i);
		if (i.patt instanceof TypedPattern)
			visitTypeReference(((TypedPattern)i.patt).type, true);
	}

	public void leaveObjectMethod(ObjectMethod meth) {
		visitor.leaveObjectMethod(meth);
	}

	public void leaveStandaloneMethod(StandaloneMethod meth) {
		visitor.leaveStandaloneMethod(meth);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			return; // not for generation
		rememberCaller(fn);
		visitor.visitFunction(fn);
		traverseFnOrMethod(fn);
		leaveFunction(fn);
		rememberCaller(null);
	}

	@Override
	public void visitTuple(TupleAssignment e) {
		visitor.visitTuple(e);
		visitExpr(e.expr, 0);
		tupleExprComplete(e);
		for (TupleMember mbr : e.members)
			visitTupleMember(mbr);
		leaveTuple(e);
	}

	@Override
	public void tupleExprComplete(TupleAssignment e) {
		visitor.tupleExprComplete(e);
	}

	@Override
	public void leaveTuple(TupleAssignment e) {
		visitor.leaveTuple(e);
	}

	@Override
	public void visitTupleMember(TupleMember sd) {
		visitor.visitTupleMember(sd);
		leaveTupleMember(sd);
	}

	@Override
	public void leaveTupleMember(TupleMember sd) {
		visitor.leaveTupleMember(sd);
	}

	private void traverseFnOrMethod(LogicHolder sd) {
		if (wantHSI) {
			List<Slot> slots = sd.slots();
			((HSIVisitor)visitor).hsiArgs(slots);
			hsiLogger.info("traversing HSI for " + sd.name().uniqueName());
//			sd.hsiTree().dump("");
			visitHSI(new VarMapping(), "", slots, sd.hsiCases());
			hsiLogger.info("finished HSI for " + sd.name().uniqueName());
		} else {
			if (patternsTree)
				visitPatternsInTreeOrder(sd);
			visitLogic(sd);
		} 
	}
	
	private void visitLogic(LogicHolder fn) {
		if (wantHSI)
			throw new NotImplementedException("We should not call visitLogic from visitHSI");
		
		if (fn instanceof FunctionDefinition) {
			for (FunctionIntro i : ((FunctionDefinition) fn).intros())
				visitFunctionIntro(i);
		} else if (fn instanceof ObjectActionHandler) {
			if (!patternsTree)
				visitPatterns((PatternsHolder)fn);
			ObjectActionHandler oah = (ObjectActionHandler)fn;
			if (!oah.guards.isEmpty())
				visitObjectGuards(oah.guards);
			else
				visitObjectsMessages(oah.messages());
		} else
			throw new NotImplementedException();
	}

	private void visitObjectGuards(List<GuardedMessages> guards) {
		for (GuardedMessages gm : guards)
			visitGuardedMessage(gm);
	}

	@Override
	public void visitGuardedMessage(GuardedMessages gm) {
		visitor.visitGuardedMessage(gm);
		visitExpr(gm.guard, 0);
		visitObjectsMessages(gm.messages());
		leaveGuardedMessage(gm);
	}

	public void leaveGuardedMessage(GuardedMessages gm) {
		visitor.leaveGuardedMessage(gm);
	}

	private void visitObjectsMessages(List<ActionMessage> messages) {
		for (ActionMessage msg : messages)
			visitMessage(msg);
	}

	private void visitPatternsInTreeOrder(LogicHolder fn) {
		hsiLogger.info("visiting patterns for " + fn.name().uniqueName());
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		HSITree hsiTree = fn.hsiTree();
		for (int i=0;i<hsiTree.width();i++) {
			hsiLogger.info("  visiting pattern " + i);
			HSIOptions tree = hsiTree.get(i);
			ArgSlot as = new ArgSlot(i, tree);
			tov.argSlot(as);
			visitPatternTree("    ", tree);
			tov.endArg(as);
		}
		hsiLogger.info("finished patterns for " + fn.name().uniqueName());
	}

	private void visitPatternTree(String indent, HSIOptions hsiOptions) {
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		for (StructDefn t : hsiOptions.ctors()) {
			hsiLogger.info(indent + "visiting ctor " + t.signature());
			// visit(t) // establishing a context
			HSICtorTree cm = (HSICtorTree) hsiOptions.getCM(t);
			tov.matchConstructor(t);
			for (int i=0;i<cm.width();i++) {
				String fld = cm.getField(i);
				StructField tf = t.findField(fld);
				tov.matchField(tf);
				visitPatternTree(indent + "  ", cm.get(i));
				tov.endField(tf);
			}
			tov.endConstructor(t);
		}
		for (NamedType t : hsiOptions.types()) {
			hsiLogger.info(indent + "visiting type " + t.signature());
			for (IntroTypeVar tv : hsiOptions.typedVars(t)) {
				if (tv.tp != null)
					tov.matchType(tv.tp.type.defn(), tv.tp.var, tv.intro);
				else
					tov.matchType(t, null, tv.intro); // for constants, there is no var to bind
			}
		}
		for (IntroVarName iv : hsiOptions.vars()) {
			hsiLogger.info(indent + "visiting var " + iv.var.uniqueName());
			tov.varInIntro(iv.var, iv.vp, iv.intro);
		}
	}

	public void rememberCaller(StandaloneDefn fn) {
		this.currentFunction = fn;
	}

	private static class SlotVar {
		private final Slot s;
		private final VarName var;

		public SlotVar(Slot s, VarName var) {
			this.s = s;
			this.var = var;
		}
	}
	
	public static class VarMapping {
		private Map<FunctionIntro, List<SlotVar>> map = new HashMap<>();
		
		public VarMapping remember(Slot s, HSIOptions opts, HSICases intros) {
			VarMapping ret = new VarMapping();
			ret.map.putAll(map);
			for (IntroVarName v : opts.vars(intros)) {
				if (!ret.map.containsKey(v.intro))
					ret.map.put(v.intro, new ArrayList<>());
				ret.map.get(v.intro).add(new SlotVar(s, v.var));
			}
			return ret;
		}

		public void bindFor(HSIVisitor hsi, FunctionIntro intro) {
			List<SlotVar> vars = map.get(intro);
			if (vars != null) {
				for (SlotVar sv : vars)
					hsi.bind(sv.s, sv.var.var);
			}
		}
	}

	public void visitHSI(VarMapping vars, String indent, List<Slot> slots, HSICases intros) {
		indent += "  ";
		HSIVisitor hsi = (HSIVisitor) visitor;
		if (slots.isEmpty()) {
			if (intros.noRemainingCases()) {
				hsiLogger.info(indent + "no slots, no cases");
				hsi.errorNoCase();
			} else if (intros.singleton()) {
				hsiLogger.info(indent + "no slots, one case ... " + intros.onlyIntro());
				if (intros.isFunction()) {
					FunctionIntro intro = intros.onlyIntro();
					vars.bindFor(hsi, intro);
					handleInline(intro);
				} else
					throw new NotImplementedException("We need to handle object methods");
			} else
				throw new NotImplementedException("I think this is an error");
		} else {
			Slot s = selectSlot(slots);
			List<Slot> remaining = new ArrayList<>(slots);
			remaining.remove(s);
			hsiLogger.info(indent + "selected slot " + s + " remaining = " + remaining + " intros = " + intros);
			HSIOptions opts = s.getOptions();
			vars = vars.remember(s, opts, intros);
			boolean wantSwitch = opts.hasSwitches(intros);
			if (wantSwitch) {
				hsi.switchOn(s);
				for (StructDefn c : opts.ctors()) {
					hsi.withConstructor(c.name.uniqueName());
					HSICtorTree cm = (HSICtorTree) opts.getCM(c);
					HSICases retainedIntros = intros.retain(cm.intros());
					hsiLogger.info(indent + "considering ctor " + c.name().uniqueName() + " intros = " + retainedIntros);
					List<Slot> extended = new ArrayList<>(remaining);
					for (int i=0;i<cm.width();i++) {
						String fld = cm.getField(i);
						HSIOptions oi = cm.get(i);
						CMSlot fieldSlot = new CMSlot(s.id()+"_"+fld, oi);
						hsi.constructorField(s, fld, fieldSlot);
						extended.add(fieldSlot);
					}
//					ArrayList<FunctionIntro> intersect = new ArrayList<>(intros);
//					intersect.retainAll(cm.intros());
					visitHSI(vars, indent, extended, retainedIntros);
				}
				for (NamedType ty : opts.types()) {
					String name = ty.name().uniqueName();
					hsi.withConstructor(name);
					HSICases intersect = intros.retain(opts.getIntrosForType(ty));
					hsiLogger.info(indent + "considering type " + ty.name().uniqueName() + " intros = " + intersect);
					if ("Number".equals(name)) {
						Set<Integer> numbers = opts.numericConstants(intersect);
						if (!numbers.isEmpty()) {
							for (int k : numbers) {
								hsi.matchNumber(k);
								HSICases forConst = intersect.retain(opts.getIntrosForType(ty));
								visitHSI(vars, indent, remaining, intersect);
								intersect.remove(forConst);
							}
							hsi.matchDefault();
						}
					}
					if ("String".equals(name)) {
						Set<String> strings = opts.stringConstants(intersect);
						if (!strings.isEmpty()) {
							for (String k : strings) {
								hsi.matchString(k);
								HSICases forConst = intersect.retain(opts.getIntrosForType(ty));
								visitHSI(vars, indent, remaining, intersect);
								intersect.remove(forConst);
							}
							hsi.matchDefault();
						}
					}
					if (intersect.noRemainingCases())
						hsi.errorNoCase();
					else
						visitHSI(vars, indent, remaining, intersect);
				}
			}
			HSICases intersect = intros.retain(opts.getDefaultIntros(intros));
			if (wantSwitch)
				hsi.defaultCase();
			hsiLogger.info(indent + "considering default case: intros = " + intersect);
			visitHSI(vars, indent, remaining, intersect);
			if (wantSwitch) {
				hsi.endSwitch();
			}
		}
	}
	
	public static Slot selectSlot(List<Slot> slots) {
		if (slots.size() == 1)
			return slots.get(0);
		int which = 0;
		int score = -1;
		int i = 0;
		for (Slot s : slots) {
			int ms = s.score();
			if (ms > score) {
				which = i;
				score = ms;
			}
			i++;
		}
		return slots.get(which);
	}

	private void handleInline(FunctionIntro i) {
		if (i == null)
			return;
		startInline(i);
		visitFunctionCases(i);
		endInline(i);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro i) {
		visitor.visitFunctionIntro(i);
		if (!patternsTree)
			visitPatterns(i);
		visitFunctionCases(i);
		leaveFunctionIntro(i);
	}

	private void visitFunctionCases(FunctionIntro i) {
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
	}

	// is public because this is a useful entry point for unit testing
	public void visitPatterns(PatternsHolder fn) {
		if (wantNestedPatterns && currentFunction != null) {
			NamedType sh = (NamedType) currentFunction.state();
			if (sh != null && !(sh instanceof ObjectDefn)) {
				TypeReference tr = new TypeReference(fn.location(), sh.name().baseName());
				tr.bind(sh);
				visitPattern(new TypedPattern(fn.location(), tr, new VarName(fn.location(), fn.name(), "_this")), true);
			}
			NestedVarReader nv = currentFunction.nestedVars();
			if (nv != null) {
				for (Pattern p : nv.patterns())
					visitPattern(p, true);
			}
		}
		for (Pattern p : fn.args())
			visitPattern(p, false);
		if (fn instanceof HandlerHolder) {
			VarPattern h = ((HandlerHolder)fn).handler();
			if (h != null)
				visitPattern(h, false);
		}
	}

	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		visitor.leaveFunctionIntro(fi);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		visitor.leaveFunction(fn);
	}

	@Override
	public void visitMessages(Messages messages) {
		visitor.visitMessages(messages);
		for (Expr e : messages.exprs)
			visitExpr(e, 0);
		leaveMessages(messages);
	}

	@Override
	public void leaveMessages(Messages msgs) {
		visitor.leaveMessages(msgs);
	}

	@Override
	public void visitMessage(ActionMessage msg) {
		visitor.visitMessage(msg);
		if (msg instanceof AssignMessage)
			visitAssignMessage((AssignMessage)msg);
		else if (msg instanceof SendMessage)
			visitSendMessage((SendMessage)msg);
		else
			throw new NotImplementedException();
			
		leaveMessage(msg);
	}

	@Override
	public void visitAssignMessage(AssignMessage msg) {
		visitor.visitAssignMessage(msg);
		visitExpr(msg.expr, 0);
		visitAssignSlot(msg.slot);
		leaveAssignMessage(msg);
	}

	@Override
	public void visitAssignSlot(Expr slot) {
		visitor.visitAssignSlot(slot);
		visitExpr(slot, 0);
	}

	@Override
	public void leaveAssignMessage(AssignMessage msg) {
		visitor.leaveAssignMessage(msg);
	}

	@Override
	public void visitSendMessage(SendMessage msg) {
		visitor.visitSendMessage(msg);
		visitExpr(msg.expr, 0);
		leaveSendMessage(msg);
	}

	@Override
	public void leaveSendMessage(SendMessage msg) {
		visitor.leaveSendMessage(msg);
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
		visitor.leaveMessage(msg);
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		visitor.leaveFunctionGroup(grp);
	}

	@Override
	public void visitPattern(Pattern p, boolean isNested) {
		visitor.visitPattern(p, isNested);
		if (p instanceof VarPattern)
			visitVarPattern((VarPattern) p, isNested);
		else if (p instanceof TypedPattern)
			visitTypedPattern((TypedPattern)p, isNested);
		else if (p instanceof ConstructorMatch)
			visitConstructorMatch((ConstructorMatch)p, isNested);
		else if (p instanceof ConstPattern)
			visitConstPattern((ConstPattern)p, isNested);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Pattern not handled: " + p.getClass());
		leavePattern(p, isNested);
	}

	@Override
	public void visitVarPattern(VarPattern p, boolean isNested) {
		visitor.visitVarPattern(p, isNested);
		visitPatternVar(p.varLoc, p.var);
	}

	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
		visitor.visitTypedPattern(p, isNested);
		visitTypeReference(p.type, true);
		visitPatternVar(p.var.loc, p.var.var);
	}

	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		visitor.visitConstructorMatch(p, isNested);
		for (Field f : p.args) {
			visitConstructorField(f.field, f.patt, isNested);
		}
		leaveConstructorMatch(p);
	}

	@Override
	public void visitConstructorField(String field, Pattern patt, boolean isNested) {
		visitor.visitConstructorField(field, patt, isNested);
		visitPattern(patt, isNested);
		leaveConstructorField(field, patt);
	}

	@Override
	public void leaveConstructorField(String field, Object patt) {
		visitor.leaveConstructorField(field, patt);
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
		visitor.leaveConstructorMatch(p);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		visitor.visitPatternVar(varLoc, var);
	}

	@Override
	public void visitConstPattern(ConstPattern p, boolean isNested) {
		visitor.visitConstPattern(p, isNested);
	}

	@Override
	public void leavePattern(Object patt, boolean isNested) {
		visitor.leavePattern(patt, isNested);
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
		visitor.visitCase(c);
		if (c.guard != null) {
			visitGuard(c);
			visitExpr(c.guard, 0);
			leaveGuard(c);
		}
		visitExpr(c.expr, 0);
		visitor.leaveCase(c);
	}

	public void visitGuard(FunctionCaseDefn c) {
		visitor.visitGuard(c);
	}

	public void leaveGuard(FunctionCaseDefn c) {
		visitor.leaveGuard(c);
	}

	public void leaveCase(FunctionCaseDefn c) {
		visitor.leaveCase(c);
	}

	@Override
	public void startInline(FunctionIntro fi) {
		visitor.startInline(fi);
	}

	@Override
	public void endInline(FunctionIntro fi) {
		visitor.endInline(fi);
	}

	@Override
	public void visitExpr(Expr expr, int nargs) {
		if (!isNeedingEnhancement(expr, nargs) && !convertedMemberExpr(expr))
			visitor.visitExpr(expr, nargs);
		if (expr == null)
			return;
		else if (expr instanceof ApplyExpr)
			visitApplyExpr((ApplyExpr)expr);
		else if (expr instanceof StringLiteral)
			visitStringLiteral((StringLiteral)expr);
		else if (expr instanceof NumericLiteral)
			visitNumericLiteral((NumericLiteral)expr);
		else if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr, nargs);
		else if (expr instanceof AnonymousVar)
			visitAnonymousVar((AnonymousVar) expr);
		else if (expr instanceof IntroduceVar)
			visitIntroduceVar((IntroduceVar) expr);
		else if (expr instanceof UnresolvedOperator)
			visitUnresolvedOperator((UnresolvedOperator) expr, nargs);
		else if (expr instanceof MemberExpr)
			visitMemberExpr((MemberExpr)expr);
		else if (expr instanceof Messages)
			visitMessages((Messages)expr);
		else if (expr instanceof MakeSend)
			visitMakeSend((MakeSend)expr);
		else if (expr instanceof MakeAcor)
			visitMakeAcor((MakeAcor)expr);
		else if (expr instanceof CurrentContainer)
			visitCurrentContainer((CurrentContainer)expr);
		else if (expr instanceof CheckTypeExpr)
			visitCheckTypeExpr((CheckTypeExpr)expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		visitor.visitCheckTypeExpr(expr);
		visitTypeReference(expr.type, false);
		visitExpr(expr.expr, 0);
		leaveCheckTypeExpr(expr);
	}

	@Override
	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
		visitor.leaveCheckTypeExpr(expr);
	}
	
	private boolean isNeedingEnhancement(Expr expr, int nargs) {
		if (!wantNestedPatterns)
			return false;
		Expr fn;
		if (expr instanceof ApplyExpr)
			fn = (Expr) ((ApplyExpr)expr).fn;
		else if (expr instanceof UnresolvedVar && nargs == 0)
			fn = expr;
		else
			return false;
		return isFnNeedingNesting(fn) != null || containedState(fn) != null;
	}

	private boolean convertedMemberExpr(Expr expr) {
		return wantHSI && expr instanceof MemberExpr;
	}
	
	public void visitApplyExpr(ApplyExpr expr) {
		ApplyExpr ae = expr;
		Expr fn = (Expr) expr.fn;
		if (wantNestedPatterns && !isConverted) {
			StateHolder sh = containedState(fn);
			List<Object> args = new ArrayList<>();
			if (sh != null) {
				// this is not good enough because it may be passed in as arg 0 to us
				args.add(new CurrentContainer(fn.location(), (NamedType) sh));
			}
			NestedVarReader nv = isFnNeedingNesting(fn);
			if (nv != null) {
				for (UnresolvedVar uv : nv.vars())
					args.add(uv);
			}
			if (!args.isEmpty()) {
				args.addAll(expr.args);
				ae = new ApplyExpr(expr.location, fn, args);
			}
			
			if (isNeedingEnhancement(expr, 0))
				visitor.visitExpr(ae, 0);
		}
		
		if (fn instanceof UnresolvedOperator && ((UnresolvedOperator)fn).op.equals("->")) {
			visitHandleExpr(fn.location(), (Expr)ae.args.get(0), (Expr)ae.args.get(1));
		} else {
			visitor.visitApplyExpr(ae);
			visitExpr(fn, ae.args.size());
			for (Object x : ae.args)
				visitExpr((Expr) x, 0);
			leaveApplyExpr(ae);
		}
	}

	@Override
	public void visitHandleExpr(InputPosition location, Expr expr, Expr handler) {
		visitor.visitHandleExpr(location, expr, handler);
		visitExpr(expr, 0);
		visitExpr(handler, 0);
		leaveHandleExpr(expr, handler);
	}

	@Override
	public void leaveHandleExpr(Expr expr, Expr handler) {
		visitor.leaveHandleExpr(expr, handler);
	}

	private NestedVarReader isFnNeedingNesting(Expr fn) {
		if (fn instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar)fn;
			if (uv.defn() instanceof StandaloneDefn)
				return ((StandaloneDefn)uv.defn()).nestedVars();
		}
		return null;
	}

	private StateHolder containedState(Expr fn) {
		if (fn instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar)fn;
			if (uv.defn() instanceof StandaloneDefn)
				return ((StandaloneDefn)uv.defn()).state();
		}
		return null;
	}


	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		visitor.leaveApplyExpr(expr);
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		if (wantHSI) {
			// generate the converted code
			if (expr.isConverted()) {
				visitConvertedExpr(expr);
			}
			else
				throw new NotImplementedException("You need to convert this expression");
		} else {
			visitor.visitMemberExpr(expr);
			visitExpr(expr.from, 0);
			if (visitMemberFields)
				visitExpr(expr.fld, 0);
			leaveMemberExpr(expr);
		}
	}

	public void visitConvertedExpr(MemberExpr expr) {
		visitor.visitConvertedExpr(expr);
		isConverted = true;
		visitExpr(expr.converted(), 0);
		leaveConvertedExpr(expr);
	}

	public void leaveConvertedExpr(MemberExpr expr) {
		isConverted = false;
		visitor.leaveConvertedExpr(expr);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		visitor.leaveMemberExpr(expr);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (nargs == 0 && wantNestedPatterns) {
			StateHolder sh = containedState(var);
			List<Object> args = new ArrayList<>();
			if (sh != null) {
				// this is not good enough because it may be passed in as arg 0 to us
				args.add(new CurrentContainer(var.location(), (NamedType) sh));
			}
			NestedVarReader nv = isFnNeedingNesting(var);
			if (nv != null && !nv.vars().isEmpty()) {
				args.addAll(nv.vars());
			}
			if (!args.isEmpty()) {
				ApplyExpr ae = new ApplyExpr(var.location, var, args);
				visitor.visitExpr(ae, 0);
				visitor.visitApplyExpr(ae);
				visitor.visitExpr(var, args.size());
				visitor.visitUnresolvedVar(var, args.size());
				for (Object v : args) {
					visitor.visitExpr((Expr) v, 0);
					if (v instanceof UnresolvedVar)
						visitor.visitUnresolvedVar((UnresolvedVar) v, 0);
					else if (v instanceof CurrentContainer)
						visitor.visitCurrentContainer((CurrentContainer) v);
					else
						throw new NotImplementedException();
				}
				visitor.leaveApplyExpr(ae);
				return; // don't just visit the var ...
			}
		}
		if (isNeedingEnhancement(var, nargs))
			visitor.visitExpr(var, nargs);
		visitor.visitUnresolvedVar(var, nargs);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		visitor.visitUnresolvedOperator(operator, nargs);
	}

	@Override
	public void visitIntroduceVar(IntroduceVar var) {
		visitor.visitIntroduceVar(var);
	}

	@Override
	public void visitAnonymousVar(AnonymousVar var) {
		visitor.visitAnonymousVar(var);
	}

	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys) {
		visitor.visitTypeReference(var, expectPolys);
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		visitor.visitStringLiteral(expr);
	}

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		visitor.visitNumericLiteral(expr);
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
		visitor.visitMakeSend(expr);
		visitExpr(expr.obj, 0);
		if (expr.handler != null)
			visitExpr(expr.handler, 0);
		leaveMakeSend(expr);
	}
	
	@Override
	public void leaveMakeSend(MakeSend expr) {
		visitor.leaveMakeSend(expr);
	}
	
	@Override
	public void visitMakeAcor(MakeAcor expr) {
		visitor.visitMakeAcor(expr);
		visitExpr(expr.obj, 0);
		leaveMakeAcor(expr);
	}

	@Override
	public void leaveMakeAcor(MakeAcor expr) {
		visitor.leaveMakeAcor(expr);
	}

	@Override
	public void visitCurrentContainer(CurrentContainer expr) {
		visitor.visitCurrentContainer(expr);
	}
	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
		visitor.visitUnitTestPackage(e);
		for (UnitDataDeclaration udd : e.decls())
			visitUnitDataDeclaration(udd);
		for (UnitTestCase c : e.tests())
			visitUnitTest(c);
		leaveUnitTestPackage(e);
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		visitor.visitUnitTest(e);
		for (UnitTestStep s : e.steps) {
			visitUnitTestStep(s);
		}
		leaveUnitTest(e);
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		visitor.leaveUnitTest(e);
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		visitor.visitUnitTestStep(s);
		if (s instanceof UnitTestAssert)
			visitUnitTestAssert((UnitTestAssert) s);
		else if (s instanceof UnitTestShove)
			visitUnitTestShove((UnitTestShove)s);
		else if (s instanceof UnitTestInvoke)
			visitUnitTestInvoke((UnitTestInvoke) s);
		else if (s instanceof UnitDataDeclaration)
			visitUnitDataDeclaration((UnitDataDeclaration) s);
		else if (s instanceof UnitTestExpect)
			visitUnitTestExpect((UnitTestExpect) s);
		else if (s instanceof UnitTestSend)
			visitUnitTestSend((UnitTestSend)s);
		else if (s instanceof UnitTestEvent)
			visitUnitTestEvent((UnitTestEvent)s);
		else if (s instanceof UnitTestMatch)
			visitUnitTestMatch((UnitTestMatch)s);
		else if (s instanceof UnitTestNewDiv)
			visitUnitTestNewDiv((UnitTestNewDiv)s);
		else
			throw new NotImplementedException("cannot handle " + s.getClass());
	}

	@Override
	public void visitUnitTestNewDiv(UnitTestNewDiv s) {
		visitor.visitUnitTestNewDiv(s);
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		visitor.visitUnitDataDeclaration(udd);
		visitTypeReference(udd.ofType, true);
		if (udd.expr != null)
			visitExpr(udd.expr, 0);
		for (Assignment f : udd.fields)
			visitUnitDataField(f);
		leaveUnitDataDeclaration(udd);
	}

	@Override
	public void visitUnitDataField(Assignment assign) {
		visitor.visitUnitDataField(assign);
		visitExpr(assign.value, 0);
		leaveUnitDataField(assign);
	}

	public void leaveUnitDataField(Assignment assign) {
		visitor.leaveUnitDataField(assign);
	}

	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		visitor.leaveUnitDataDeclaration(udd);
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		visitor.visitUnitTestAssert(a);
		visitAssertExpr(true, a.value);
		visitAssertExpr(false, a.expr);
		postUnitTestAssert(a);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		visitor.visitAssertExpr(isValue, e);
		visitExpr(e, 0);
		leaveAssertExpr(isValue, e);
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
		visitor.leaveAssertExpr(isValue, e);
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		visitor.postUnitTestAssert(a);
	}

	@Override
	public void visitUnitTestShove(UnitTestShove s) {
		visitor.visitUnitTestShove(s);
		for (UnresolvedVar v : s.slots) {
			visitShoveSlot(v);
		}
		visitShoveExpr(s.value);
		leaveUnitTestShove(s);
	}

	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		visitor.visitShoveSlot(v);
	}

	@Override
	public void visitShoveExpr(Expr value) {
		visitor.visitShoveExpr(value);
		visitExpr(value, 0);
	}

	@Override
	public void leaveUnitTestShove(UnitTestShove s) {
		visitor.leaveUnitTestShove(s);
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		visitor.visitUnitTestInvoke(uti);
		if (wantHSI) {
			// generate the converted code
			if (uti.isConverted())
				visitExpr(uti.converted(), 0);
			else
				throw new NotImplementedException("You need to convert this UTI");
		} else {
			visitExpr(uti.expr, 0);
		}
		leaveUnitTestInvoke(uti);
	}

	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		visitor.leaveUnitTestInvoke(uti);
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect ute) {
		visitor.visitUnitTestExpect(ute);
		visitUnresolvedVar(ute.ctr, 0);
		for (Expr e : ute.args)
			visitExpr(e, 0);
		expectHandlerNext();
		visitExpr(ute.handler, 0);
		leaveUnitTestExpect(ute);
	}

	@Override
	public void expectHandlerNext() {
		visitor.expectHandlerNext();
	}

	@Override
	public void leaveUnitTestExpect(UnitTestExpect ute) {
		visitor.leaveUnitTestExpect(ute);
	}

	public void visitUnitTestSend(UnitTestSend s) {
		visitor.visitUnitTestSend(s);
		visitUnresolvedVar(s.card, 0);
		visitTypeReference(s.contract, true);
		visitSendExpr(s.contract, s.expr);
		leaveUnitTestSend(s);
	}

	private void visitSendExpr(TypeReference contract, Expr expr) {
		if (contract.defn() == null)
			return;
		if (expr instanceof UnresolvedVar) {
			visitor.visitSendMethod(contract.defn(), (UnresolvedVar)expr);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			UnresolvedVar te;
			Expr h = null;
			List<Object> args;
			if (ae.fn instanceof UnresolvedOperator && ((UnresolvedOperator)ae.fn).op.equals("->")) {
				te = (UnresolvedVar) ae.args.get(0);
				h = (Expr) ae.args.get(1);
				args = new ArrayList<>();
			} else {
				te = (UnresolvedVar)ae.fn;
				args = ae.args;
			}
			visitSendMethod(contract.defn(), te);
			for (Object e : args)
				visitExpr((Expr) e, 0);
			if (h != null)
				visitExpr(h, 0);
		} else
			throw new NotImplementedException("I don't think that should happen");
	}
	
	

	public void visitSendMethod(NamedType defn, UnresolvedVar expr) {
		visitor.visitSendMethod(defn, expr);
	}

	public void leaveUnitTestSend(UnitTestSend s) {
		visitor.leaveUnitTestSend(s);
	}

	public void visitUnitTestEvent(UnitTestEvent e) {
		visitor.visitUnitTestEvent(e);
		visitExpr(e.card, 0);
		visitExpr(e.expr, 0);
		leaveUnitTestEvent(e);
	}

	public void leaveUnitTestEvent(UnitTestEvent e) {
		visitor.leaveUnitTestEvent(e);
	}

	public void visitUnitTestMatch(UnitTestMatch m) {
		visitor.visitUnitTestMatch(m);
		visitExpr(m.card, 0);
		leaveUnitTestMatch(m);
	}

	public void leaveUnitTestMatch(UnitTestMatch m) {
		visitor.leaveUnitTestMatch(m);
	}

	@Override
	public void leaveUnitTestPackage(UnitTestPackage e) {
		visitor.leaveUnitTestPackage(e);
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
		if (!cd.generate)
			return;
		visitor.visitContractDecl(cd);
		for (ContractMethodDecl m : cd.methods)
			visitContractMethod(m);
		leaveContractDecl(cd);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		visitor.visitContractMethod(cmd);
		for (Object a : cmd.args) {
			if (a instanceof TypedPattern) {
				TypedPattern p = (TypedPattern) a;
				visitTypeReference(p.type, true);
			}
		}
		if (cmd.handler != null)
			visitTypeReference(cmd.handler.type, true);
		leaveContractMethod(cmd);

	}

	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		visitor.leaveContractMethod(cmd);
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
		visitor.leaveContractDecl(cd);
	}
	
	public void traversalDone() {
		visitor.traversalDone();
	}

	@Override
	public String toString() {
		return "Traverser{" + visitor + "}";
	}
}
