package test.lifting;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.Lifter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.Repository;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class CollectingNestedVariableReferences {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	Repository r = new Repository();
	Lifter l = new RepositoryLifter();
	HSIVisitor v = context.mock(HSIVisitor.class);
	
	@SuppressWarnings("unchecked")
	@Test
	public void localReferencesAreNotAddedAgain() {
		FunctionName name = FunctionName.function(pos, pkg, "f");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		List<Object> args = new ArrayList<>();
		VarPattern vp = new VarPattern(pos, new VarName(pos, name, "x"));
		args.add(vp);
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(vp);
		FunctionIntro fi = new FunctionIntro(name, args);
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, vr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsiTree = new HSIArgsTree(1);
		hsiTree.consider(fi);
		hsiTree.get(0).addVar(vp.name(), fi);
		fn.bindHsi(hsiTree);
		r.addEntry(name, fn);
		
		l.lift(r);
		
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			allowing(v).isHsi(); will(returnValue(true));
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).hsiArgs((List<Slot>) with(Matchers.hasSize(1))); inSequence(seq);
			oneOf(v).bind((Slot) with(any(Slot.class)), with("x")); inSequence(seq);
			oneOf(v).startInline(fi); inSequence(seq);
			oneOf(v).visitExpr(vr, 0); inSequence(seq);
			oneOf(v).visitUnresolvedVar(vr, 0); inSequence(seq);
			oneOf(v).endInline(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		r.traverse(v);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void parentReferenceIsAddedToADirectChildUsingIt() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		StringLiteral sl = new StringLiteral(pos, "hello");
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(vp);

		FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
		{
			List<Object> args = new ArrayList<>();
			args.add(vp);
			FunctionIntro fi = new FunctionIntro(nameF, args);
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, sl);
			fi.functionCase(fcd);
			fnF.intro(fi);
			HSIArgsTree hsiTree = new HSIArgsTree(1);
			hsiTree.consider(fi);
			hsiTree.get(0).addVar(vp.name(), fi);
			fnF.bindHsi(hsiTree);
			r.addEntry(nameF, fnF);
		}
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fnG = new FunctionDefinition(nameG, 0);
		{
			FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, vr);
			fi.functionCase(fcd);
			fnG.intro(fi);
			HSIArgsTree hsiTree = new HSIArgsTree(0);
			fnG.bindHsi(hsiTree);
			r.addEntry(nameG, fnG);
		}
		
		l.lift(r);
		
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			allowing(v).isHsi(); will(returnValue(true));
			oneOf(v).visitFunction(fnF); inSequence(seq);
			oneOf(v).hsiArgs((List<Slot>) with(Matchers.hasSize(1))); inSequence(seq);
			oneOf(v).bind((Slot) with(any(Slot.class)), with("x")); inSequence(seq);
			oneOf(v).startInline(with(any(FunctionIntro.class))); inSequence(seq);
			oneOf(v).visitExpr(sl, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(sl); inSequence(seq);
			oneOf(v).endInline(with(any(FunctionIntro.class))); inSequence(seq);
			oneOf(v).leaveFunction(fnF); inSequence(seq);
			
			oneOf(v).visitFunction(fnG); inSequence(seq);
			oneOf(v).hsiArgs((List<Slot>) with(Matchers.hasSize(1))); inSequence(seq);
			oneOf(v).bind((Slot) with(any(Slot.class)), with("x")); inSequence(seq);
			oneOf(v).startInline(with(any(FunctionIntro.class))); inSequence(seq);
			oneOf(v).visitExpr(vr, 0); inSequence(seq);
			oneOf(v).visitUnresolvedVar(vr, 0); inSequence(seq);
			oneOf(v).endInline(with(any(FunctionIntro.class))); inSequence(seq);
			oneOf(v).leaveFunction(fnG); inSequence(seq);
		}});
		r.traverse(v);
	}
	
	// TODO: need to handle recursive cases
}
