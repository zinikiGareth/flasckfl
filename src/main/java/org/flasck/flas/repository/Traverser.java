package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.ParenExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.DeferMeException;
import org.flasck.flas.compiler.modules.TraverserModule;
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
import org.flasck.flas.parsedForm.CastExpr;
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
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.CardBinding;
import org.flasck.flas.parsedForm.assembly.LibraryAssembly;
import org.flasck.flas.parsedForm.assembly.RoutingAction;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestClose;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestExpectCancel;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.parsedForm.ut.UnitTestInput;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestNewDiv;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
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
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements RepositoryVisitor {
	final static Logger logger = LoggerFactory.getLogger("Traverser");
	final static Logger patternsLogger = LoggerFactory.getLogger("TOPatterns");
	final static Logger tcLogger = LoggerFactory.getLogger("TypeChecker");
	final static Logger hsiLogger = LoggerFactory.getLogger("HSI");
	private final RepositoryVisitor visitor;
	private LogicHolder currentFunction;
	private FunctionGroups functionOrder;
	private boolean wantImplementedMethods = false;
	private boolean wantNestedPatterns;
	private boolean wantHSI;
	private boolean wantEventSources = false;
	private boolean wantMethodMembersWithObjects = false;
	private boolean visitMemberFields = false;
	private PackageName onlyPackage;
	private boolean isConverted;
	private boolean currFnHasState;
	private Comparator<NamedType> unionLastOrder = new Comparator<NamedType>() {
		@Override
		public int compare(NamedType o1, NamedType o2) {
			// Any comes dead last
			boolean o1isA = o1 == LoadBuiltins.any;
			boolean o2isA = o2 == LoadBuiltins.any;
			if (o1isA && o2isA) {
				return 0;
			} else if (o1isA) {
				return 1;
			} else if (o2isA)
				return -1;
			
			// Unions come after any structs
			boolean o1isU = o1 instanceof UnionTypeDefn;
			boolean o2isU = o2 instanceof UnionTypeDefn;
			if (!o1isU && o2isU)
				return -1;
			else if (o1isU && !o2isU)
				return 1;
			return NamedType.nameComparator.compare(o1, o2);
		}
	};
	private Repository repository;
	private Iterable<TraverserModule> modules = null;

	public Traverser(RepositoryVisitor visitor) {
		this.visitor = visitor;
		this.modules = ServiceLoader.load(TraverserModule.class);
	}

	public Traverser withImplementedMethods() {
		this.wantImplementedMethods = true;
		return this;
	}

	public Traverser withObjectMethods() {
		this.wantMethodMembersWithObjects = true;
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

	public Traverser withMemberFields() {
		this.visitMemberFields = true;
		return this;
	}

	public Traverser withEventSources() {
		this.wantEventSources = true;
		return this;
	}

	public Traverser forPackage(PackageName pkg) {
		this.onlyPackage = pkg;
		return this;
	}

	public void doTraversal(Repository repository) {
		this.repository = repository;
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
						patternsLogger.info("aborted processing " + ex.toString());
						((StackVisitor)visitor).reduceTo(1);
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
		logger.debug("about to visit " + entriesInSomeOrder);
		for (RepositoryEntry e : entriesInSomeOrder) {
			logger.debug("visiting " + e);
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
		else if (!isDesiredPackage(e))
			return;
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
			if (functionOrder == null)
				visitTuple((TupleAssignment)e);
		} else if (e instanceof TupleMember) {
			// if needed, it should be visited within the assignment
		} else if (e instanceof ObjectMethod) {
			if (functionOrder == null)
				visitObjectMethod((ObjectMethod)e);
		} else if (e instanceof ObjectAccessor) {
			if (functionOrder == null)
				visitObjectAccessor((ObjectAccessor)e);
		} else if (e instanceof ObjectCtor) {
			if (functionOrder == null)
				visitObjectCtor((ObjectCtor)e);
		} else if (e instanceof StandaloneMethod) {
			if (functionOrder == null)
				visitStandaloneMethod((StandaloneMethod)e);
		} else if (e instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) e;
			visitHandlerImplements(hi);
		} else if (e instanceof StructDefn) {
			StructDefn s = (StructDefn)e;
			if (s.generate)
				visitStructDefn(s);
		} else if (e instanceof UnionTypeDefn) {
			UnionTypeDefn u = (UnionTypeDefn)e;
			if (u.generate)
				visitUnionTypeDefn(u);
		} else if (e instanceof UnitTestPackage)
			visitUnitTestPackage((UnitTestPackage)e);
		else if (e instanceof SystemTest)
			visitSystemTest((SystemTest)e);
		else if (e instanceof UnitDataDeclaration) {
			; // even top level ones are in a package ...
//			UnitDataDeclaration udd = (UnitDataDeclaration) e;
//			if (udd.isTopLevel())
//				visitUnitDataDeclaration(udd);
		} else if (e instanceof StructField) {
			visitStructFieldAccessor((StructField) e);
		} else if (e instanceof ApplicationAssembly) {
			visitAssembly((ApplicationAssembly) e);
		} else if (e instanceof LibraryAssembly) {
			;
		} else if (e instanceof ApplicationRouting) {
			; 
		} else if (e instanceof VarPattern || e instanceof TypedPattern || e instanceof IntroduceVar || e instanceof HandlerLambda ||
				   e instanceof PolyType || e instanceof RequiresContract || e instanceof Provides || e instanceof ObjectContract || e instanceof ImplementsContract ||
				   e instanceof Template) {
			; // do nothing: these are just in the repo for lookup purposes
		} else if (e instanceof ContractMethodDecl) {
			; // do nothing; added to repository for consistency reasons
		} else if (modules != null) {
			boolean done = false;
			for (TraverserModule m : modules) {
				if (done = m.visitEntry(this, visitor, e))
					break;
			}
			if (!done)
				throw new NotImplementedException("cannot handle " + e.getClass());
		} else
			throw new NotImplementedException("traverser cannot handle " + e.getClass());
	}

	private boolean isDesiredPackage(RepositoryEntry e) {
		if (onlyPackage == null)
			return true;
		else if (onlyPackage.baseName() == null)
			return e.name().packageName().baseName() == null;
		else
			return onlyPackage.baseName().equals(e.name().packageName().baseName());
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
		visitTypeReference(sf.type, true, -1);
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
			visitTypeReference(c, true, -1);
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
//		for (HandlerImplements ic : obj.handlers)
//			visitHandlerImplements(ic, obj);
		for (Template t : obj.templates) {
			visitTemplate(t, false);
		}
		if (wantMethodMembersWithObjects) {
			for (ObjectCtor c : obj.ctors)
				visitObjectCtor(c);
			for (ObjectAccessor c : obj.acors)
				visitObjectAccessor(c);
			for (ObjectMethod m : obj.methods)
				visitObjectMethod(m);
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
		visitTypeReference(oc.implementsType(), true, -1);
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
//		for (HandlerImplements ic : cd.handlers)
//			visitHandlerImplements(ic, cd);
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
		leaveAgentDefn(s);
	}

	public void visitAssembly(ApplicationAssembly e) {
		visitor.visitAssembly(e);
		if (e.routing() != null)
			visitApplicationRouting(e.routing());
		leaveAssembly(e);
	}

	public void leaveAssembly(ApplicationAssembly e) {
		visitor.leaveAssembly(e);
	}

	public void visitApplicationRouting(ApplicationRouting e) {
		visitor.visitApplicationRouting(e);
		visitRoutingCommon(e);
		leaveApplicationRouting(e);
	}

	public void leaveApplicationRouting(ApplicationRouting e) {
		visitor.leaveApplicationRouting(e);
	}

	public void visitSubRouting(SubRouting r) {
		visitor.visitSubRouting(r);
		visitRoutingCommon(r);
		leaveSubRouting(r);
	}

	public void leaveSubRouting(SubRouting r) {
		visitor.leaveSubRouting(r);
	}
	
	private void visitRoutingCommon(SubRouting r) {
		for (CardBinding cb : r.assignments) {
			visitCardAssignment(cb);
		}
		visitActions(r.enter);
		visitActions(r.at);
		visitActions(r.exit);
		for (SubRouting sr : r.routes) {
			visitSubRouting(sr);
		}
	}
	
	@Override
	public void visitActions(RoutingActions actions) {
		if (actions == null)
			return;
		visitor.visitActions(actions);
		if (actions.actions != null) {
			for (RoutingAction a : actions.actions)
				visitRoutingAction(a);
		}
		leaveActions(actions);
	}

	public void visitRoutingAction(RoutingAction a) {
		visitor.visitRoutingAction(a);
		visitTypeReference(a.contract, false, -1);
		visitExpr(a.card, 0);
		int pos = 0;
		for (Expr e : a.exprs) {
			visitRoutingExpr(a, pos++, e);
		}
		leaveRoutingAction(a);
	}

	public void visitRoutingExpr(RoutingAction a, int pos, Expr e) {
		visitor.visitRoutingExpr(a, pos, e);
		visitExpr(e, 0);
		leaveRoutingExpr(a, pos, e);
	}
	
	@Override
	public void leaveRoutingExpr(RoutingAction a, int pos, Expr e) {
		visitor.leaveRoutingExpr(a, pos, e);
	}

	public void leaveRoutingAction(RoutingAction a) {
		visitor.leaveRoutingAction(a);
	}

	@Override
	public void leaveActions(RoutingActions actions) {
		visitor.leaveActions(actions);
	}

	public void visitCardAssignment(CardBinding card) {
		visitor.visitCardAssignment(card);
		visitTypeReference(card.cardType, false, 0);
		leaveCardAssignment(card);
	}

	public void leaveCardAssignment(CardBinding card) {
		visitor.leaveCardAssignment(card);
	}

	@Override
	public void visitServiceDefn(ServiceDefinition s) {
		visitor.visitServiceDefn(s);
		for (RequiresContract rc : s.requires)
			visitRequires(rc);
		for (Provides p : s.provides)
			visitProvides(p);
//		for (HandlerImplements ic : s.handlers)
//			visitHandlerImplements(ic, null);
		leaveServiceDefn(s);
	}

	public void visitProvides(Provides p) {
		visitor.visitProvides(p);
		visitTypeReference(p.implementsType(), true, -1);
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
		visitTypeReference(rc.implementsType(), true, -1);
	}

	public void visitImplements(ImplementsContract ic) {
		visitor.visitImplements(ic);
		visitTypeReference(ic.implementsType(), true, -1);
		if (wantImplementedMethods) {
			for (ObjectMethod om : ic.implementationMethods)
				visitObjectMethod(om);
		}
		leaveImplements(ic);
	}

	public void leaveImplements(ImplementsContract ic) {
		visitor.leaveImplements(ic);
	}

	public void visitHandlerImplements(HandlerImplements hi) {
		visitor.visitHandlerImplements(hi);
		visitTypeReference(hi.implementsType(), true, -1);
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
				visitTypeReference(ty, true, -1);
		}
		afterTemplateChainTypes(t);
		NestingChain chain = t.nestingChain();
		if (chain != null) {
			for (TypeReference ty : chain.types())
				visitTypeReference(ty, true, -1);
		}
		for (TemplateBinding b : t.bindings()) {
			visitTemplateBinding(b);
		}
		for (TemplateStylingOption tso : t.stylings())
			visitTemplateStyling(tso);
		leaveTemplate(t);
	}

	public void afterTemplateChainTypes(Template t) {
		visitor.afterTemplateChainTypes(t);
	}

	public void leaveTemplate(Template t) {
		visitor.leaveTemplate(t);
	}

	public void visitTemplateBinding(TemplateBinding b) {
		currFnHasState = true;
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
		if (tso.orelse != null) {
			visitTemplateStylesElse(tso);
			for (Expr e : tso.orelse) {
				if (!(e instanceof StringLiteral))
					visitTemplateStyleExpr(e);				
			}
		}
		for (TemplateStylingOption tsoi : tso.conditionalStylings)
			visitTemplateStyling(tsoi);
		for (TemplateEvent te : tso.events)
			visitTemplateEvent(te);
		leaveTemplateStyling(tso);
	}

	public void visitTemplateStylesElse(TemplateStylingOption tso) {
		visitor.visitTemplateStylesElse(tso);
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
		currFnHasState = true;
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
		if (!oc.generate)
			return;
		currFnHasState = true;
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
		if (grp.size() == 1 && !grp.functions().iterator().next().generate())
			return;
		tcLogger.info("Checking group " + grp.functions());
		visitor.visitFunctionGroup(grp);
		// visit the patterns for all the cases first
		for (LogicHolder sd : grp.functions()) {
			tcLogger.info("  processing patterns for " + sd.name().uniqueName());
			if (sd instanceof FunctionDefinition)
				visitor.visitFunction((FunctionDefinition) sd);
			else if (sd instanceof ObjectCtor)
				visitor.visitObjectCtor((ObjectCtor) sd);
			else if (sd instanceof ObjectMethod) {
				ObjectMethod meth = (ObjectMethod) sd;
				visitor.visitObjectMethod(meth);
				if (meth.args().isEmpty() && !meth.hasMessages()) {
					visitor.leaveObjectMethod(meth);
					continue;
				}
			} else if (sd instanceof StandaloneMethod) {
				visitor.visitStandaloneMethod((StandaloneMethod) sd);
				visitor.visitObjectMethod(((StandaloneMethod) sd).om);
			} else if (sd instanceof TupleAssignment || sd instanceof TupleMember)
				continue;
			else
				throw new NotImplementedException(sd.getClass().getName());
			visitPatternsInTreeOrder(sd);
			if (sd instanceof FunctionDefinition)
				visitor.leaveFunction((FunctionDefinition) sd);
			else if (sd instanceof ObjectCtor)
				visitor.leaveObjectCtor((ObjectCtor) sd);
			else if (sd instanceof ObjectMethod) {
				ObjectMethod meth = (ObjectMethod) sd;
				if (wantEventSources && meth.isEvent()) {
					for (Template e : meth.eventSourceExprs()) {
						visitEventSource(e);
					}
				}
				if (!meth.args().isEmpty() || meth.hasMessages()) {
					if (meth.hasImplements() && meth.getImplements() instanceof HandlerImplements)
						traverseHandlerLambdas((HandlerImplements)meth.getImplements());
				}
				visitor.leaveObjectMethod(meth);
			} else if (sd instanceof StandaloneMethod) {
				visitor.leaveObjectMethod(((StandaloneMethod) sd).om);
				visitor.leaveStandaloneMethod((StandaloneMethod) sd);
			}
		}
		// then visit the logic
		for (LogicHolder sd : grp.functions()) {
			tcLogger.info("  processing logic for " + sd.name().uniqueName());
			if (sd instanceof FunctionDefinition)
				visitFunction((FunctionDefinition) sd);
			else if (sd instanceof StandaloneMethod)
				visitStandaloneMethod((StandaloneMethod) sd);
			else if (sd instanceof ObjectMethod)
				visitObjectMethod((ObjectMethod) sd);
			else if (sd instanceof ObjectCtor)
				visitObjectCtor((ObjectCtor) sd);
			else if (sd instanceof TupleAssignment)
				visitTuple((TupleAssignment) sd);
			else if (sd instanceof TupleMember)
				continue;
			else
				throw new NotImplementedException("visit " + sd.getClass());
		}
		leaveFunctionGroup(grp);
	}

	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
		currFnHasState = meth.hasState();
		rememberCaller(meth);
		visitor.visitStandaloneMethod(meth);
		visitObjectMethod(meth.om);
		leaveStandaloneMethod(meth);
		rememberCaller(null);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		if (!meth.generate)
			return;
		currFnHasState = meth.hasState();
		visitor.visitObjectMethod(meth);
		if (functionOrder == null && meth.hasImplements() && meth.getImplements() instanceof HandlerImplements)
			traverseHandlerLambdas((HandlerImplements)meth.getImplements());
		traverseFnOrMethod(meth);
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
			visitTypeReference(((TypedPattern)i.patt).type, true, -1);
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
		for (FunctionIntro i : fn.intros()) // don't process if broken to avoid cascades
			if (i.broken())
				return;
		currFnHasState = fn.hasState();
		rememberCaller(fn);
		visitor.visitFunction(fn);
		traverseFnOrMethod(fn);
		leaveFunction(fn);
		rememberCaller(null);
	}

	@Override
	public void visitTuple(TupleAssignment e) {
		currFnHasState = e.hasState();
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
			visitHSI(sd);
		} else {
			visitLogic(sd);
		} 
	}

	public void visitHSI(LogicHolder sd) {
		List<Slot> slots = sd.slots();
		((HSIVisitor)visitor).hsiArgs(slots);
		hsiLogger.info("traversing HSI for " + sd.name().uniqueName());
//			sd.hsiTree().dump("");
		visitHSI(new VarMapping(), "", slots, sd.hsiCases(), null, new BackupPlan(), new DontConsiderAgain());
		hsiLogger.info("finished HSI for " + sd.name().uniqueName());
	}
	
	public void visitLogic(LogicHolder fn) {
		if (wantHSI)
			throw new NotImplementedException("We should not call visitLogic from visitHSI");
		
		if (fn instanceof FunctionDefinition) {
			for (FunctionIntro i : ((FunctionDefinition) fn).intros()) {
				if (functionOrder != null)
					patternsLogger.debug("processing intro " + i);
				visitFunctionIntro(i);
			}
		} else if (fn instanceof ObjectActionHandler) {
			if (functionOrder == null)
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
		patternsLogger.info("visiting patterns for " + fn.name().uniqueName());
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		HSITree hsiTree = fn.hsiTree();
		for (int i=0;i<hsiTree.width();i++) {
			HSIOptions tree = hsiTree.get(i);
			patternsLogger.info("  visiting pattern " + i + " with " + tree.introNames());
			ArgSlot as = new ArgSlot(i, tree);
			tov.argSlot(as);
			visitPatternTree("    " + i, tree);
			tov.endArg(as);
		}
		tov.patternsDone(fn);
		patternsLogger.info("finished patterns for " + fn.name().uniqueName());
	}

	private void visitPatternTree(String indent, HSIOptions hsiOptions) {
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		for (StructDefn t : hsiOptions.ctors()) {
			// visit(t) // establishing a context
			HSICtorTree cm = (HSICtorTree) hsiOptions.getCM(t);
			patternsLogger.info(indent + ": visiting ctor " + t.signature() + " with intros " + cm.introNames());
			tov.matchConstructor(t);
			for (int i=0;i<cm.width();i++) {
				String fld = cm.getField(i);
				StructField tf = t.findField(fld);
				tov.matchField(tf);
				visitPatternTree("  " + indent + "." + t.signature()+"."+cm.slot(i), cm.get(i));
				tov.endField(tf);
			}
			tov.endConstructor(t);
		}
		for (NamedType t : hsiOptions.types()) {
			patternsLogger.info(indent + ": visiting type " + t.signature() + " with intros " + introNames(hsiOptions.getIntrosForType(t)));
			for (IntroTypeVar tv : hsiOptions.typedVars(t)) {
				if (tv.tp != null)
					tov.matchType(tv.tp.type.namedDefn(), tv.tp.var, tv.intro);
				else
					tov.matchType(t, null, tv.intro); // for constants, there is no var to bind
			}
		}
		for (IntroVarName iv : hsiOptions.vars()) {
			String iname = "none";
			if (iv.intro != null)
				iname = iv.intro.name().uniqueName();
			patternsLogger.info(indent + ": visiting var " + iv.var.uniqueName() + " with intro " + iname);
			tov.varInIntro(iv.var, iv.vp, iv.intro);
		}
	}

	private List<String> introNames(List<FunctionIntro> intros) {
		List<String> ret = new ArrayList<>();
		for (FunctionIntro i : intros) {
			if (i == null)
				ret.add("null");
			else
				ret.add(i.name().uniqueName());
		}
		return ret;
	}

	public void rememberCaller(LogicHolder fn) {
		this.currentFunction = fn;
	}

	private static class SlotVar {
		private final Slot s;
		private final VarName var;

		public SlotVar(Slot s, VarName var) {
			this.s = s;
			this.var = var;
		}
		
		@Override
		public String toString() {
			return var.var + "<-" + s;
		}
	}
	
	public static class VarMapping {
		private Map<FunctionIntro, List<SlotVar>> map = new HashMap<>();
		
		public VarMapping remember(Slot s, HSIOptions opts, HSICases intros) {
			VarMapping ret = new VarMapping();
			ret.map.putAll(map);
			loop:
			for (IntroVarName v : opts.vars(intros)) {
				if (!ret.map.containsKey(v.intro))
					ret.map.put(v.intro, new ArrayList<>());
				List<SlotVar> vl = ret.map.get(v.intro);
				for (SlotVar sv : vl)
					if (sv.var == v.var) {
						// Because of the very complicated way in which we do the backup plans, we can't know whether we are
						// going to visit "remember" multiple times for the same case
						// For simplicity, just don't do the rebind ...
						hsiLogger.info("rebound " + v.var);
						continue loop;
					}
				vl.add(new SlotVar(s, v.var));
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
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			String sep = "";
			for (Entry<FunctionIntro, List<SlotVar>> e : map.entrySet()) {
				sb.append(sep);
				sep = ",";
				sb.append(e.getKey().name().name);
				sb.append("->");
				sb.append(e.getValue());
			}
			sb.append("}");
			return sb.toString();
		}
	}

	public void visitHSI(VarMapping vars, String indent, List<Slot> slots, HSICases intros, List<FunctionIntro> moreGeneral, BackupPlan planB, DontConsiderAgain notAgain) {
		indent += "  ";
		HSIVisitor hsi = (HSIVisitor) visitor;
		if (slots.isEmpty()) {
			if (intros.noRemainingCases()) {
				hsiLogger.info(indent + "no slots, no cases; moreGeneral = " + moreGeneral + "; backup = " + planB);
				if (moreGeneral != null && !moreGeneral.isEmpty())
					visitHSI(vars, indent + "  ", slots, new FunctionHSICases(moreGeneral), null, planB, notAgain);
				else if (!planB.hasHope())
					hsi.errorNoCase();
				else
					planB.backup(this, notAgain);
			} else if (intros.singleton()) {
				hsiLogger.info(indent + "no slots, one case ... " + intros.onlyIntro());
				inline(vars, hsi, intros.onlyIntro());
			} else {
				// In particular, have we considered "overlapping" cases where there are multiple cases that say exactly the same thing?
				hsiLogger.error("multiple cases remain in HSI processing:");
				for (String i : intros.introNames()) {
					hsiLogger.error("  " + i);
				}
				throw new HaventConsideredThisException("We should either have 0 or 1 remaining cases at this point, but there might be ways to go wrong");
			}
		} else {
			Slot s = selectSlot(slots);
			List<Slot> remaining = new ArrayList<>(slots);
			remaining.remove(s);
			hsiLogger.info(indent + "selected slot " + s + " remaining = " + remaining + " intros = " + intros);
			HSIOptions opts = s.getOptions();
			VarMapping updatedVars = vars.remember(s, opts, intros);
			if (moreGeneral != null)
				updatedVars = updatedVars.remember(s, opts, new FunctionHSICases(moreGeneral));
			hsiLogger.info(indent + "remembered vars " + updatedVars);
			boolean wantSwitch = opts.hasSwitches(intros);
			DontConsiderAgain forDef = notAgain;
			if (wantSwitch) {
				boolean needSwitch = true;
				for (StructDefn c : opts.ctors()) {
					DontConsiderAgain dca = forDef.considered(s, c);
					if (dca == null)
						continue;
					else
						forDef = dca;
					HSICtorTree cm = (HSICtorTree) opts.getCM(c);
					HSICases retainedIntros = intros.retain(cm.intros());
					if (retainedIntros.noRemainingCases()) {
						hsiLogger.info(indent + "slot " + s + ": ignoring ctor " + c.name().uniqueName() + " with no matchign intros");
						continue;
					}
					hsiLogger.info(indent + "slot " + s + ": considering ctor " + c.name().uniqueName() + " intros = " + retainedIntros);
					if (needSwitch) {
						hsi.switchOn(s);
						needSwitch = false;
					}
					hsi.withConstructor(c.name);
					BackupPlan backupPlan = new BackupPlan(updatedVars, indent, remaining);
					for (NamedType ty : opts.unionsIncluding(c))
						backupPlan.allows(opts.getIntrosForType(ty));
					backupPlan.allows(opts.getDefaultIntros(intros));
					List<FunctionIntro> bi = null;
					if (opts.types().contains(c))
						bi = opts.getIntrosForType(c);
					else
						bi = s.lessSpecific();
					List<Slot> extended = new ArrayList<>(remaining);
					for (int i=0;i<cm.width();i++) {
						String fld = cm.getField(i);
						HSIOptions oi = cm.get(i);
						CMSlot fieldSlot = new CMSlot(s.id()+"_"+fld, oi, bi);
						hsi.constructorField(s, fld, fieldSlot);
						extended.add(fieldSlot);
					}
					visitHSI(updatedVars, indent, extended, retainedIntros, bi, backupPlan, forDef);
				}
				Set<NamedType> still = new HashSet<>(opts.types());
				Set<NamedType> ordered = new TreeSet<>(unionLastOrder);
				ordered.addAll(opts.types());
				for (NamedType ty : ordered) {
					still.remove(ty);
					DontConsiderAgain dca = forDef.considered(s, ty);
					if (dca == null)
						continue;
					else
						forDef = dca;
					HSICases intersect = intros.retain(opts.getIntrosForType(ty));
					if (intersect.noRemainingCases()) {
						hsiLogger.info(indent + "slot " + s + ": ignoring type " + ty.name().uniqueName() + " with no matchign intros");
						continue;
					}
					hsiLogger.info(indent + "slot " + s + ": considering type " + ty.name().uniqueName() + " intros = " + intersect);
					NameOfThing name = ty.name();
					if (needSwitch) {
						hsi.switchOn(s);
						needSwitch = false;
					}
					hsi.withConstructor(name);
					BackupPlan backupPlan = new BackupPlan(updatedVars, indent, remaining);
					for (NamedType t2 : still)
						backupPlan.allows(opts.getIntrosForType(t2));
					backupPlan.allows(opts.getDefaultIntros(intros));
					if ("Number".equals(name.uniqueName())) {
						Set<Integer> numbers = opts.numericConstants(intersect);
						if (!numbers.isEmpty()) {
							for (int k : numbers) {
								hsi.matchNumber(k);
								HSICases forConst = intersect.retain(opts.getIntrosForNumber(k));
								intersect.remove(forConst);
								hsiLogger.info(indent + "slot " + s + ": considering number " + k + " intros = " + forConst);
								visitHSI(updatedVars, indent, remaining, forConst, moreGeneral, backupPlan, forDef);
								intersect.remove(forConst);
							}
							hsi.matchDefault();
						}
					}
					if ("String".equals(name.uniqueName())) {
						Set<String> strings = opts.stringConstants(intersect);
						if (!strings.isEmpty()) {
							for (String k : strings) {
								hsi.matchString(k);
								HSICases forConst = intersect.retain(opts.getIntrosForString(k));
								intersect.remove(forConst);
								hsiLogger.info(indent + "slot " + s + ": considering string " + k + " intros = " + forConst);
								visitHSI(updatedVars, indent, remaining, forConst, moreGeneral, backupPlan, forDef);
								intersect.remove(forConst);
							}
							hsi.matchDefault();
						}
					}
					visitHSI(updatedVars, indent, remaining, intersect, moreGeneral, backupPlan, forDef);
				}
				if (needSwitch) {
					wantSwitch = false;
				}
			}
			HSICases intersect = intros.retain(opts.getDefaultIntros(intros));
			if (wantSwitch)
				hsi.defaultCase();
			hsiLogger.info(indent + "slot " + s + ": for the default case, intros = " + intersect);
			visitHSI(updatedVars, indent, remaining, intersect, moreGeneral, planB, forDef);
			if (wantSwitch) {
				hsi.endSwitch();
			}
		}
	}

	private void inline(VarMapping vars, HSIVisitor hsi, FunctionIntro intro) {
		vars.bindFor(hsi, intro);
		handleInline(intro);
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
		if (functionOrder == null)
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
		if (wantNestedPatterns) {
			NestedVarReader nv = null;
			if (currentFunction != null) {
				NamedType sh = (NamedType) currentFunction.state();
				if (!currentFunction.isObjAccessor() && !(currentFunction instanceof ObjectCtor) && sh != null) {
					TypeReference tr = new TypeReference(fn.location(), sh.name().baseName());
					tr.bind(sh);
					visitPattern(new TypedPattern(fn.location(), tr, new VarName(fn.location(), fn.name(), "_this")), true);
				}
				nv = currentFunction.nestedVars();
			} else if (fn instanceof LogicHolder) {
				nv = ((LogicHolder)fn).nestedVars();
			}
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
		if (msg.handlerExpr() != null) {
			visitSendHandler(msg.handlerExpr());
		}
		if (msg.subscriberName() != null) {
			visitSubscriberName(msg.subscriberName());
		}
		leaveSendMessage(msg);
	}

	public void visitSendHandler(Expr handlerExpr) {
		visitor.visitSendHandler(handlerExpr);
		visitExpr(handlerExpr, 0);
		leaveSendHandler(handlerExpr);
	}

	public void leaveSendHandler(Expr handlerExpr) {
		visitor.leaveSendHandler(handlerExpr);
	}

	public void visitSubscriberName(Expr subscriberName) {
		visitor.visitSubscriberName(subscriberName);
		visitExpr(subscriberName, 0);
		leaveSubscriberName(subscriberName);
	}

	public void leaveSubscriberName(Expr handlerName) {
		visitor.leaveSubscriberName(handlerName);
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
		visitPatternVar(p.location(), p.var);
	}

	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
		visitor.visitTypedPattern(p, isNested);
		visitTypeReference(p.type, true, -1);
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
//		System.out.println("visiting case " + c + " in " + visitor);
		visitor.visitCase(c);
		if (c.guard != null) {
//			System.out.println("visiting guard " + c.guard);
			visitGuard(c);
			visitExpr(c.guard, 0);
			leaveGuard(c);
//			System.out.println("leaving guard " + c.guard);
		}
//		System.out.println("fn case expr is " + c.expr);
		visitExpr(c.expr, 0);
		visitor.leaveCase(c);
//		System.out.println("left case " + c);
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
//		System.out.println("visiting expr " + expr + " in " + visitor);
		if (expr instanceof ParenExpr) {
			visitExpr((Expr) ((ParenExpr)expr).expr, nargs);
			return;
		}
		boolean ine = isNeedingEnhancement(expr, nargs);
		boolean cme = convertedMemberExpr(expr);
		if ((!ine && !cme) || isConverted)
			visitor.visitExpr(expr, nargs);
//		else
//			System.out.println("not calling visitExpr because" + (ine?" needs enhancement":"") + (cme?" expr with conversion":"") + (isConverted?" processing conversion":""));
		if (expr == null)
			return;
		else if (expr instanceof ApplyExpr)
			visitApplyExpr((ApplyExpr)expr);
		else if (expr instanceof StringLiteral)
			visitStringLiteral((StringLiteral)expr);
		else if (expr instanceof NumericLiteral)
			visitNumericLiteral((NumericLiteral)expr);
		else if (expr instanceof TypeReference)
			visitTypeReference((TypeReference) expr, false, nargs);
		else if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr, nargs);
		else if (expr instanceof AnonymousVar)
			visitAnonymousVar((AnonymousVar) expr);
		else if (expr instanceof IntroduceVar)
			visitIntroduceVar((IntroduceVar) expr);
		else if (expr instanceof UnresolvedOperator)
			visitUnresolvedOperator((UnresolvedOperator) expr, nargs);
		else if (expr instanceof MemberExpr)
			visitMemberExpr((MemberExpr)expr, nargs);
		else if (expr instanceof Messages)
			visitMessages((Messages)expr);
		else if (expr instanceof MakeSend)
			visitMakeSend((MakeSend)expr);
		else if (expr instanceof MakeAcor)
			visitMakeAcor((MakeAcor)expr);
		else if (expr instanceof CurrentContainer)
			visitCurrentContainer((CurrentContainer)expr, false, false);
		else if (expr instanceof CheckTypeExpr)
			visitCheckTypeExpr((CheckTypeExpr)expr);
		else if (expr instanceof TypeExpr)
			visitTypeExpr((TypeExpr)expr);
		else if (expr instanceof CastExpr)
			visitCastExpr((CastExpr)expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		visitor.visitCheckTypeExpr(expr);
		visitTypeReference(expr.type, false, -1);
		visitExpr(expr.expr, 0);
		leaveCheckTypeExpr(expr);
	}

	@Override
	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
		visitor.leaveCheckTypeExpr(expr);
	}
	
	@Override
	public void visitTypeExpr(TypeExpr expr) {
		visitor.visitTypeExpr(expr);
		visitExpr(expr.type, 0);
		leaveTypeExpr(expr);
	}

	public void leaveTypeExpr(TypeExpr expr) {
		visitor.leaveTypeExpr(expr);
	}

	public void visitCastExpr(CastExpr expr) {
		visitor.visitCastExpr(expr);
		visitTypeReference(expr.type, true, -1);
		visitExpr(expr.val, 0);
		leaveCastExpr(expr);
	}

	public void leaveCastExpr(CastExpr expr) {
		visitor.leaveCastExpr(expr);
	}

	private boolean isNeedingEnhancement(Expr expr, int nargs) {
		if (!wantNestedPatterns)
			return false;
		Expr fn;
		if (expr instanceof ApplyExpr)
			fn = (Expr) ((ApplyExpr)expr).fn;
		else if ((expr instanceof UnresolvedVar || expr instanceof TypeReference) && nargs == 0)
			fn = expr;
		else
			return false;
		return isFnNeedingNesting(fn) != null || containingMe(fn) != null;
	}

	private boolean convertedMemberExpr(Expr expr) {
		return wantHSI && expr instanceof MemberExpr;
	}
	
	public void visitApplyExpr(ApplyExpr expr) {
		ApplyExpr ae = expr;
//		System.out.println("visiting apply expr " + expr + " in " + visitor);
		Expr fn = (Expr) expr.fn;
		if (wantNestedPatterns && !isConverted) {
			NamedType sh = containingMe(fn);
			List<Object> args = new ArrayList<>();
			if (sh != null && currFnHasState && (!(fn instanceof UnresolvedVar) || !(((UnresolvedVar)fn).defn() instanceof ObjectCtor))) {
				// this is not good enough because it may be passed in as arg 0 to us
				args.add(new CurrentContainer(fn.location(), sh));
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
		
		visitor.visitApplyExpr(ae);
		visitExpr(fn, ae.args.size());
		for (Object x : ae.args)
			visitExpr((Expr) x, 0);
		leaveApplyExpr(ae);
	}

	private NestedVarReader isFnNeedingNesting(Expr fn) {
		Object defn;
		if (fn instanceof UnresolvedVar) {
			defn = ((UnresolvedVar)fn).defn();
		} else if (fn instanceof TypeReference) {
			defn = ((TypeReference)fn).namedDefn();
		} else
			return null;
		if (defn instanceof LogicHolder)
			return ((LogicHolder)defn).nestedVars();
		else if (defn instanceof HandlerImplements)
			return ((HandlerImplements)defn).nestedVars();
		else
			return null;
	}

	private NamedType containingMe(Expr fn) {
		Object defn;
		if (fn instanceof UnresolvedVar) {
			defn = ((UnresolvedVar)fn).defn();
		} else if (fn instanceof TypeReference) {
			defn = ((TypeReference)fn).namedDefn();
		} else
			return null;
		if (defn instanceof LogicHolder) {
			return containingMe(((LogicHolder)defn).name());
		} else
			return null;
	}

	private NamedType containingMe(FunctionName n) {
		NameOfThing o = n.container();
		if (o == null || o instanceof PackageName)
			return null;
		else if (o instanceof FunctionName)
			return containingMe((FunctionName)o);
		else if (o instanceof CardName || o instanceof ObjectName)
			return repository.get(o.uniqueName());
		else if (o instanceof CSName || o instanceof HandlerName) {
			RepositoryEntry ret = repository.get(o.uniqueName());
			if (ret == null)
				throw new CantHappenException("there was no type for " + o.uniqueName());
			if (ret instanceof NamedType)
				return (NamedType) ret;
			else
				throw new NotImplementedException(ret + " for " + o + " was not a namedType");
		} else
			throw new NotImplementedException("o is " + o.getClass() + ": " + o.uniqueName());
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		visitor.leaveApplyExpr(expr);
	}

	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		if (wantHSI) {
			// generate the converted code
			if (expr.isConverted()) {
				visitConvertedExpr(expr, nargs);
			}
			else
				throw new NotImplementedException("You need to convert this expression: " + expr);
			return true;
		} else {
			boolean done = visitor.visitMemberExpr(expr, nargs);
			if (!done) {
				visitExpr(expr.from, 0);
				if (visitMemberFields)
					visitExpr(expr.fld, 0);
			}
			leaveMemberExpr(expr, done);
			return done;
		}
	}

	public void visitConvertedExpr(MemberExpr expr, int nargs) {
		visitor.visitConvertedExpr(expr, nargs);
		isConverted = true;
		visitExpr(expr.converted(), nargs);
		leaveConvertedExpr(expr);
	}

	public void leaveConvertedExpr(MemberExpr expr) {
		isConverted = false;
		visitor.leaveConvertedExpr(expr);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr, boolean done) {
		visitor.leaveMemberExpr(expr, done);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (nargs == 0 && wantNestedPatterns) {
			NamedType sh = containingMe(var);
			List<Object> args = new ArrayList<>();
			CurrentContainer cc = null;
			if (sh != null && currFnHasState && !(var.defn() instanceof ObjectCtor)) {
				cc = new CurrentContainer(var.location(), sh);
				// this is not good enough because it may be passed in as arg 0 to us
				args.add(cc);
			}
			NestedVarReader nv = isFnNeedingNesting(var);
			if (nv != null && !nv.vars().isEmpty()) {
				args.addAll(nv.vars());
			}
			if (!args.isEmpty()) {
				ApplyExpr ae = new ApplyExpr(var.location, var, args);
				boolean wouldWantState = ae.fn instanceof UnresolvedVar && (((UnresolvedVar)ae.fn).defn() instanceof FunctionDefinition || ((UnresolvedVar)ae.fn).defn() instanceof StandaloneMethod);
				visitor.visitExpr(ae, 0);
				visitor.visitApplyExpr(ae);
				visitor.visitExpr(var, args.size());
				visitor.visitUnresolvedVar(var, args.size());
				for (Object v : args) {
					visitor.visitExpr((Expr) v, 0);
					if (v instanceof UnresolvedVar)
						visitor.visitUnresolvedVar((UnresolvedVar) v, 0);
					else if (v instanceof CurrentContainer)
						visitor.visitCurrentContainer((CurrentContainer) v, v == cc, wouldWantState);
					else
						throw new NotImplementedException();
				}
				visitor.leaveApplyExpr(ae);
				return; // we are done now - don't do the regular 'visit var' thing
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
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		visitor.visitTypeReference(var, expectPolys, exprNargs);
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
		if (expr.handlerName != null)
			visitExpr(expr.handlerName, 0);
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
	public void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState) {
		visitor.visitCurrentContainer(expr, isObjState, wouldWantState);
	}
	
	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
		currFnHasState = false;
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
	public void leaveUnitTest(TestStepHolder e) {
		visitor.leaveUnitTest(e);
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		visitor.visitUnitTestStep(s);
		if (s instanceof UnitTestAssert)
			visitUnitTestAssert((UnitTestAssert) s);
		else if (s instanceof UnitTestIdentical)
			visitUnitTestIdentical((UnitTestIdentical)s);
		else if (s instanceof UnitTestClose)
			visitUnitTestClose((UnitTestClose)s);
		else if (s instanceof UnitTestShove)
			visitUnitTestShove((UnitTestShove)s);
		else if (s instanceof UnitTestInvoke)
			visitUnitTestInvoke((UnitTestInvoke) s);
		else if (s instanceof UnitDataDeclaration)
			visitUnitDataDeclaration((UnitDataDeclaration) s);
		else if (s instanceof UnitTestExpect)
			visitUnitTestExpect((UnitTestExpect) s);
		else if (s instanceof UnitTestExpectCancel)
			visitUnitTestExpectCancel((UnitTestExpectCancel) s);
		else if (s instanceof UnitTestSend)
			visitUnitTestSend((UnitTestSend)s);
		else if (s instanceof UnitTestRender)
			visitUnitTestRender((UnitTestRender)s);
		else if (s instanceof UnitTestEvent)
			visitUnitTestEvent((UnitTestEvent)s);
		else if (s instanceof UnitTestInput)
			visitUnitTestInput((UnitTestInput)s);
		else if (s instanceof UnitTestMatch)
			visitUnitTestMatch((UnitTestMatch)s);
		else if (s instanceof UnitTestNewDiv)
			visitUnitTestNewDiv((UnitTestNewDiv)s);
		else if (modules != null) {
			boolean done = false;
			for (TraverserModule m : modules) {
				if (done = m.visitUnitTestStep(this, visitor, s))
					break;
			}
			if (!done)
				throw new NotImplementedException("cannot handle " + s.getClass());
		} else
			throw new NotImplementedException("cannot handle " + s.getClass());
		leaveUnitTestStep(s);
	}

	@Override
	public void leaveUnitTestStep(UnitTestStep s) {
		visitor.leaveUnitTestStep(s);
	}

	@Override
	public void visitUnitTestNewDiv(UnitTestNewDiv s) {
		visitor.visitUnitTestNewDiv(s);
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		visitor.visitUnitDataDeclaration(udd);
		visitTypeReference(udd.ofType, true, -1);
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
	public void visitUnitTestIdentical(UnitTestIdentical a) {
		visitor.visitUnitTestIdentical(a);
		visitAssertExpr(true, a.value);
		visitAssertExpr(false, a.expr);
		postUnitTestIdentical(a);
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
	public void postUnitTestIdentical(UnitTestIdentical a) {
		visitor.postUnitTestIdentical(a);
	}

	@Override
	public void visitUnitTestClose(UnitTestClose s) {
		visitor.visitUnitTestClose(s);
		visitExpr(s.card, 0);
		leaveUnitTestClose(s);
	}

	@Override
	public void leaveUnitTestClose(UnitTestClose s) {
		visitor.leaveUnitTestClose(s);
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
	public void visitUnitTestExpectCancel(UnitTestExpectCancel utec) {
		visitor.visitUnitTestExpectCancel(utec);
		visitUnresolvedVar(utec.handlerName, 0);
		leaveUnitTestExpectCancel(utec);
	}

	@Override
	public void expectHandlerNext() {
		visitor.expectHandlerNext();
	}

	@Override
	public void leaveUnitTestExpect(UnitTestExpect ute) {
		visitor.leaveUnitTestExpect(ute);
	}

	public void leaveUnitTestExpectCancel(UnitTestExpectCancel utec) {
		visitor.leaveUnitTestExpectCancel(utec);
	}

	public void visitUnitTestSend(UnitTestSend s) {
		visitor.visitUnitTestSend(s);
		visitUnresolvedVar(s.card, 0);
		visitTypeReference(s.contract, true, -1);
		visitSendExpr(s.contract, s.expr, s.handler);
		leaveUnitTestSend(s);
	}

	private void visitSendExpr(TypeReference contract, Expr expr, UnresolvedVar handler) {
		if (contract.namedDefn() == null)
			return;
		if (expr instanceof UnresolvedVar) {
			visitor.visitSendMethod(contract.namedDefn(), (UnresolvedVar)expr);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			visitSendMethod(contract.namedDefn(), (UnresolvedVar) ae.fn);
			for (Object e : ae.args)
				visitExpr((Expr) e, 0);
		} else
			throw new CantHappenException("the expr in send is not a var or an apply");
		if (handler != null)
			visitExpr(handler, 0);
	}
	
	

	public void visitSendMethod(NamedType defn, UnresolvedVar expr) {
		visitor.visitSendMethod(defn, expr);
	}

	public void leaveUnitTestSend(UnitTestSend s) {
		visitor.leaveUnitTestSend(s);
	}

	public void visitUnitTestRender(UnitTestRender e) {
		visitor.visitUnitTestRender(e);
		visitExpr(e.card, 0);
		leaveUnitTestRender(e);
	}

	@Override
	public void leaveUnitTestRender(UnitTestRender e) {
		visitor.leaveUnitTestRender(e);
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

	public void visitUnitTestInput(UnitTestInput e) {
		visitor.visitUnitTestInput(e);
		visitExpr(e.card, 0);
		visitExpr(e.expr, 0);
		leaveUnitTestInput(e);
	}

	public void leaveUnitTestInput(UnitTestInput e) {
		visitor.leaveUnitTestInput(e);
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
	public void visitSystemTest(SystemTest st) {
		visitor.visitSystemTest(st);
		if (st.configure != null)
			visitSystemTestStage(st.configure);
		for (SystemTestStage s : st.stages) {
			visitSystemTestStage(s);
		}
		if (st.cleanup != null)
			visitSystemTestStage(st.cleanup);
		leaveSystemTest(st);
	}
	
	public void visitSystemTestStage(SystemTestStage s) {
		visitor.visitSystemTestStage(s);
		for (UnitTestStep step : s.steps) {
			visitUnitTestStep(step);
		}
		leaveSystemTestStage(s);
	}

	@Override
	public void leaveSystemTestStage(SystemTestStage s) {
		visitor.leaveSystemTestStage(s);
	}

	@Override
	public void leaveSystemTest(SystemTest st) {
		visitor.leaveSystemTest(st);
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
				visitTypeReference(p.type, true, -1);
			}
		}
		if (cmd.handler != null)
			visitTypeReference(cmd.handler.type, true, -1);
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
