package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PatternsHolder;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
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
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.patterns.HSICtorTree;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIOptions.IntroTypeVar;
import org.flasck.flas.patterns.HSIOptions.IntroVarName;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements Visitor {
	private final Visitor visitor;
	private StandaloneDefn currentFunction;
	private FunctionGroups functionOrder;
	private boolean wantNestedPatterns;
	private boolean wantHSI;
	private boolean patternsTree;
	private boolean visitMemberFields = false;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
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

	public void doTraversal(Repository repository) {
		if (functionOrder != null) {
			for (FunctionGroup grp : functionOrder)
				visitFunctionGroup(grp);
		}
		for (RepositoryEntry e : repository.dict.values()) {
			visitEntry(e);
		}
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
		else if (e instanceof FunctionDefinition) {
			if (functionOrder == null)
				visitFunction((FunctionDefinition)e);
		} else if (e instanceof ObjectMethod) {
			if (functionOrder == null)
				visitObjectMethod((ObjectMethod)e);
		} else if (e instanceof StandaloneMethod) {
			if (functionOrder == null)
				visitStandaloneMethod((StandaloneMethod)e);
		} else if (e instanceof StructDefn)
			visitStructDefn((StructDefn)e);
		else if (e instanceof UnionTypeDefn)
			visitUnionTypeDefn((UnionTypeDefn)e);
		else if (e instanceof UnitTestPackage)
			visitUnitTestPackage((UnitTestPackage)e);
		else if (e instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) e;
			if (udd.isTopLevel())
				visitUnitDataDeclaration(udd);
		} else if (e instanceof VarPattern || e instanceof TypedPattern)
			; // do nothing: it is just in the repo for lookup purposes
		else if (e instanceof CurryArgument)
			; // do nothing; just for resolution
		else
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
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
		visitor.leaveStructDefn(s);
	}

	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		visitor.visitUnionTypeDefn(ud);
//		for (StructField f : s.fields)
//			visitStructField(f);
		leaveUnionTypeDefn(ud);
	}
	
	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		visitor.leaveUnionTypeDefn(ud);
	}

	@Override
	public void visitObjectDefn(ObjectDefn e) {
		visitor.visitObjectDefn(e);
	}

	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
		visitor.visitFunctionGroup(grp);
		for (StandaloneDefn sd : grp.functions()) {
			if (sd instanceof FunctionDefinition)
				visitFunction((FunctionDefinition) sd);
			else if (sd instanceof StandaloneMethod)
				visitStandaloneMethod((StandaloneMethod) sd);
			else
				throw new NotImplementedException();
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
		if (!meth.args().isEmpty() || !meth.messages().isEmpty()) {
			traverseFnOrMethod(meth);
		}
		leaveObjectMethod(meth);
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

	private void traverseFnOrMethod(LogicHolder sd) {
		if (wantHSI) {
			List<Slot> slots = sd.slots();
			((HSIVisitor)visitor).hsiArgs(slots);
			visitHSI(new VarMapping(), slots, sd.hsiCases());
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
		} else if (fn instanceof ObjectMethod) {
			if (!patternsTree)
				visitPatterns((PatternsHolder)fn);
			visitObjectsMessages(((ObjectMethod)fn).messages());
		} else
			throw new NotImplementedException();
	}

	private void visitObjectsMessages(List<ActionMessage> messages) {
		for (ActionMessage msg : messages)
			visitMessage(msg);
	}

	private void visitPatternsInTreeOrder(LogicHolder fn) {
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		HSITree hsiTree = fn.hsiTree();
		for (int i=0;i<hsiTree.width();i++) {
			HSIOptions tree = hsiTree.get(i);
			ArgSlot as = new ArgSlot(i, tree);
			tov.argSlot(as);
			visitPatternTree(tree);
			tov.endArg(as);
		}
	}

	private void visitPatternTree(HSIOptions hsiOptions) {
		TreeOrderVisitor tov = (TreeOrderVisitor)visitor;
		for (StructDefn t : hsiOptions.ctors()) {
			// visit(t) // establishing a context
			HSICtorTree cm = (HSICtorTree) hsiOptions.getCM(t);
			tov.matchConstructor(t);
			for (int i=0;i<cm.width();i++) {
				String fld = cm.getField(i);
				StructField tf = t.findField(fld);
				tov.matchField(tf);
				visitPatternTree(cm.get(i));
				tov.endField(tf);
			}
			tov.endConstructor(t);
		}
		for (NamedType t : hsiOptions.types()) {
			for (IntroTypeVar tv : hsiOptions.typedVars(t)) {
				if (tv.tp != null)
					tov.matchType(t, tv.tp.var, tv.intro);
				else
					tov.matchType(t, null, tv.intro); // for constants, there is no var to bind
			}
		}
		for (IntroVarName iv : hsiOptions.vars()) {
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

	public void visitHSI(VarMapping vars, List<Slot> slots, HSICases intros) {
		HSIVisitor hsi = (HSIVisitor) visitor;
		if (slots.isEmpty()) {
			if (intros.noRemainingCases())
				hsi.errorNoCase();
			else if (intros.singleton()) {
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
			HSIOptions opts = s.getOptions();
			vars = vars.remember(s, opts, intros);
			boolean wantSwitch = opts.hasSwitches(intros);
			if (wantSwitch) {
				hsi.switchOn(s);
				for (StructDefn c : opts.ctors()) {
					hsi.withConstructor(c.name.uniqueName());
					HSICtorTree cm = (HSICtorTree) opts.getCM(c);
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
					visitHSI(vars, extended, intros.retain(cm.intros()));
				}
				for (NamedType ty : opts.types()) {
					String name = ty.name().uniqueName();
					hsi.withConstructor(name);
					HSICases intersect = intros.retain(opts.getIntrosForType(ty));
					if ("Number".equals(name)) {
						Set<Integer> numbers = opts.numericConstants(intersect);
						if (!numbers.isEmpty()) {
							for (int k : numbers) {
								hsi.matchNumber(k);
								HSICases forConst = intersect.retain(opts.getIntrosForType(ty));
								visitHSI(vars, remaining, intersect);
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
								visitHSI(vars, remaining, intersect);
								intersect.remove(forConst);
							}
							hsi.matchDefault();
						}
					}
					if (intersect.noRemainingCases())
						hsi.errorNoCase();
					else
						visitHSI(vars, remaining, intersect);
				}
			}
			HSICases intersect = intros.retain(opts.getDefaultIntros(intros));
			if (wantSwitch)
				hsi.defaultCase();
			visitHSI(vars, remaining, intersect);
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

	// useful for unit testing
	public void visitPatterns(PatternsHolder fn) {
		if (wantNestedPatterns && currentFunction != null) {
			NestedVarReader nv = currentFunction.nestedVars();
			if (nv != null) {
				for (Pattern p : nv.patterns())
					visitPattern(p, true);
			}
		}
		for (Pattern p : fn.args())
			visitPattern(p, false);
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
	public void visitAssignSlot(List<UnresolvedVar> slot) {
		visitor.visitAssignSlot(slot);
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
		visitTypeReference(p.type);
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
			visitor.visitGuard(c);
			visitExpr(c.guard, 0);
			visitor.leaveGuard(c);
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
		if (!isNeedingEnhancement(expr, nargs))
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
		else if (expr instanceof UnresolvedOperator)
			visitUnresolvedOperator((UnresolvedOperator) expr, nargs);
		else if (expr instanceof MemberExpr)
			visitMemberExpr((MemberExpr)expr);
		else if (expr instanceof Messages)
			visitMessages((Messages)expr);
		else if (expr instanceof MakeSend)
			visitMakeSend((MakeSend)expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	private boolean isNeedingEnhancement(Expr expr, int nargs) {
		if (!wantNestedPatterns)
			return false;
		if (expr instanceof ApplyExpr && isFnNeedingNesting((Expr) ((ApplyExpr)expr).fn) != null)
			return true;
		if (expr instanceof UnresolvedVar && nargs == 0 && isFnNeedingNesting((UnresolvedVar)expr) != null)
			return true;
		return false;
	}

	public void visitApplyExpr(ApplyExpr expr) {
		ApplyExpr ae = expr;
		Expr fn = (Expr) expr.fn;
		if (wantNestedPatterns) {
			NestedVarReader nv = isFnNeedingNesting(fn);
			if (nv != null) {
				List<Object> args = new ArrayList<>();
				for (UnresolvedVar uv : nv.vars())
					args.add(uv);
				args.addAll(expr.args);
				ae = new ApplyExpr(expr.location, fn, args);
			}
		}
		
		visitor.visitApplyExpr(ae);
		visitExpr(fn, ae.args.size());
		for (Object x : ae.args)
			visitExpr((Expr) x, 0);
		visitor.leaveApplyExpr(ae);
	}

	private NestedVarReader isFnNeedingNesting(Expr uv) {
		if (uv instanceof UnresolvedVar) {
			UnresolvedVar fn = (UnresolvedVar)uv;
			if (fn.defn() instanceof StandaloneDefn)
				return ((StandaloneDefn)fn.defn()).nestedVars();
		}
		return null;
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		visitor.visitMemberExpr(expr);
		visitExpr(expr.from, 0);
		if (visitMemberFields)
			visitExpr(expr.fld, 0);
		leaveMemberExpr(expr);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		visitor.leaveMemberExpr(expr);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (nargs == 0 && wantNestedPatterns) {
			NestedVarReader nv = isFnNeedingNesting(var);
			if (nv != null && !nv.vars().isEmpty()) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				List<Object> args = (List)nv.vars();
				ApplyExpr ae = new ApplyExpr(var.location, var, args);
				visitor.visitExpr(ae, 0);
				visitor.visitApplyExpr(ae);
				visitor.visitExpr(var, args.size());
				visitor.visitUnresolvedVar(var, args.size());
				for (Object v : args) {
					visitor.visitExpr((Expr) v, 0);
					visitor.visitUnresolvedVar((UnresolvedVar) v, 0);
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
	public void visitTypeReference(TypeReference var) {
		visitor.visitTypeReference(var);
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
		visitExpr(expr.obj, 0);
		visitor.visitMakeSend(expr);
	}
	
	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
		visitor.visitUnitTestPackage(e);
		for (UnitTestCase c : e.tests())
			visitUnitTest(c);
		visitor.leaveUnitTestPackage(e);
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
		else if (s instanceof UnitDataDeclaration)
			visitUnitDataDeclaration((UnitDataDeclaration) s);
		else
			throw new NotImplementedException();
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		visitor.visitUnitDataDeclaration(udd);
		visitTypeReference(udd.ofType);
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
		visitExpr(a.value, 0);
		leaveAssertExpr(true, a.value);
		visitAssertExpr(false, a.expr);
		visitExpr(a.expr, 0);
		leaveAssertExpr(false, a.expr);
		postUnitTestAssert(a);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		visitor.visitAssertExpr(isValue, e);
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
	public void leaveUnitTestPackage(UnitTestPackage e) {
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
		visitor.visitContractDecl(cd);
		for (ContractMethodDecl m : cd.methods)
			visitContractMethod(m);
		visitor.leaveContractDecl(cd);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		visitor.visitContractMethod(cmd);
		for (Object a : cmd.args) {
			if (a instanceof TypedPattern) {
				TypedPattern p = (TypedPattern) a;
				visitTypeReference(p.type);
			}
		}
		leaveContractMethod(cmd);

	}

	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		visitor.leaveContractMethod(cmd);
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
	}
}
