package test.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.lifting.FunctionGroupOrdering;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FunctionGroupTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final Visitor v = context.mock(Visitor.class);
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	final FunctionName nameH = FunctionName.function(pos, pkg, "h");
	final FunctionName nameS1 = FunctionName.function(pos, pkg, "s1");
	FunctionDefinition fn = new FunctionDefinition(nameF, 2);
	FunctionDefinition gn = new FunctionDefinition(nameG, 2);
	FunctionDefinition hn = new FunctionDefinition(nameH, 2);
	StandaloneMethod s1 = new StandaloneMethod(new ObjectMethod(pos, nameS1, new ArrayList<>(), null));

	@Before
	public void intros() {
		fn.intro(new FunctionIntro(nameF, new ArrayList<>()));
		gn.intro(new FunctionIntro(nameG, new ArrayList<>()));
		hn.intro(new FunctionIntro(nameH, new ArrayList<>()));
	}
	
	@Test
	public void aSingleGroupOfOneFunction() {
		FunctionGroup grp1 = new DependencyGroup(fn);

		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunction(fn); inSequence(s);
			oneOf(v).visitFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(fn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp1); inSequence(s);
			oneOf(v).traversalDone();
		}});
		new Traverser(v).withFunctionsInDependencyGroups(new FunctionGroupOrdering(Arrays.asList(grp1))).doTraversal(new Repository());
	}

	@Test
	public void aSingleGroupOfTwoFunctions() {
		FunctionGroup grp1 = new DependencyGroup(fn, gn);

		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunction(fn); inSequence(s);
			oneOf(v).visitFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(fn); inSequence(s);
			
			oneOf(v).visitFunction(gn); inSequence(s);
			oneOf(v).visitFunctionIntro(gn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(gn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(gn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp1); inSequence(s);
			oneOf(v).traversalDone();
		}});
		FunctionGroupOrdering order = new FunctionGroupOrdering(Arrays.asList(grp1));
		new Traverser(v).withFunctionsInDependencyGroups(order).doTraversal(new Repository());
	}

	@Test
	public void twoGroupsOfOneFunction() {
		FunctionGroup grp1 = new DependencyGroup(fn);
		FunctionGroup grp2 = new DependencyGroup(hn);

		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunction(fn); inSequence(s);
			oneOf(v).visitFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(fn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunctionGroup(grp2); inSequence(s);
			
			oneOf(v).visitFunction(hn); inSequence(s);
			oneOf(v).visitFunctionIntro(hn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(hn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(hn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp2); inSequence(s);
			oneOf(v).traversalDone();
		}});
		FunctionGroupOrdering order = new FunctionGroupOrdering(Arrays.asList(grp1, grp2));
		new Traverser(v).withFunctionsInDependencyGroups(order).doTraversal(new Repository());
	}

	@Test
	public void twoBiggerGroups() {
		FunctionGroup grp1 = new DependencyGroup(fn, gn);
		FunctionGroup grp2 = new DependencyGroup(hn);

		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunction(fn); inSequence(s);
			oneOf(v).visitFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(fn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(fn); inSequence(s);
			
			oneOf(v).visitFunction(gn); inSequence(s);
			oneOf(v).visitFunctionIntro(gn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(gn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(gn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitFunctionGroup(grp2); inSequence(s);
			
			oneOf(v).visitFunction(hn); inSequence(s);
			oneOf(v).visitFunctionIntro(hn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunctionIntro(hn.intros().get(0)); inSequence(s);
			oneOf(v).leaveFunction(hn); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp2); inSequence(s);
			oneOf(v).traversalDone();
		}});
		FunctionGroupOrdering order = new FunctionGroupOrdering(Arrays.asList(grp1, grp2));
		new Traverser(v).withFunctionsInDependencyGroups(order).doTraversal(new Repository());
	}

	@Test
	public void aSingleGroupWithAStandaloneMethod() {
		HashSet<StandaloneDefn> ms1 = new HashSet<StandaloneDefn>();
		ms1.add(s1);
		FunctionGroup grp1 = new DependencyGroup(ms1);

		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp1); inSequence(s);
			
			oneOf(v).visitStandaloneMethod(s1); inSequence(s);
			oneOf(v).visitObjectMethod(s1.om); inSequence(s);
			oneOf(v).leaveObjectMethod(s1.om); inSequence(s);
			oneOf(v).leaveStandaloneMethod(s1); inSequence(s);
			
			oneOf(v).leaveFunctionGroup(grp1); inSequence(s);
			oneOf(v).traversalDone();
		}});
		new Traverser(v).withFunctionsInDependencyGroups(new FunctionGroupOrdering(Arrays.asList(grp1))).doTraversal(new Repository());
	}
}
