package test.lifting;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.lifting.Lifter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Repository.Visitor;
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
	Visitor v = context.mock(Visitor.class);
	
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
		r.addEntry(name, fn);
		l.lift(r);
		
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitPatternVar(pos, "x"); inSequence(seq);
		}});
		r.traverse(new LiftTestVisitor(v));
	}

	@Test
	public void parentReferenceIsAddedToADirectChildUsingIt() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));

		FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
		{
			List<Object> args = new ArrayList<>();
			args.add(vp);
			FunctionIntro fi = new FunctionIntro(nameF, args);
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fnF.intro(fi);
			r.addEntry(nameF, fnF);
		}
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fnG = new FunctionDefinition(nameG, 0);
		{
			UnresolvedVar vr = new UnresolvedVar(pos, "x");
			vr.bind(vp);
			FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, vr);
			fi.functionCase(fcd);
			fnG.intro(fi);
			r.addEntry(nameG, fnG);
		}
		
		l.lift(r);
		
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fnF); inSequence(seq);
			oneOf(v).visitPatternVar(pos, "x"); inSequence(seq);
			oneOf(v).visitFunction(fnG); inSequence(seq);
			oneOf(v).visitPatternVar(pos, "x"); inSequence(seq);
		}});
		r.traverse(new LiftTestVisitor(v));
	}
}
