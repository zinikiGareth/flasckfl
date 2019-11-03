package test.lifting;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.lifting.MappingAnalyzer;
import org.flasck.flas.lifting.MappingCollector;
import org.flasck.flas.lifting.VarDependencyMapper;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class CollectorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	private VarDependencyMapper dependencies = context.mock(VarDependencyMapper.class);

	@Test
	public void aVarPatternNotLocalToThisFunctionIsRecorded() {
		MappingCollector c = context.mock(MappingCollector.class);
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(vp);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
			oneOf(c).recordNestedVar(fi, vp);
			oneOf(dependencies).recordVarDependency(nameG, nameF, c);
		}});
		ma.visitUnresolvedVar(vr);
	}

	@Test
	public void aVarPatternLocalToThisFunctionIsIgnored() {
		MappingCollector c = context.mock(MappingCollector.class);
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameG, "x"));
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(vp);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
		}});
		ma.visitUnresolvedVar(vr);
	}

	@Test
	public void aTypedVarPatternNotLocalToThisFunctionIsRecorded() {
		MappingCollector c = context.mock(MappingCollector.class);
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number"), new VarName(pos, nameF, "x"));
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(tp);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
			oneOf(c).recordNestedVar(fi, tp);
		}});
		ma.visitUnresolvedVar(vr);
	}

	@Test
	public void aTypedVarPatternLocalToThisFunctionIsIgnored() {
		MappingCollector c = context.mock(MappingCollector.class);
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number"), new VarName(pos, nameG, "x"));
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(tp);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
		}});
		ma.visitUnresolvedVar(vr);
	}

	@Test
	public void aConstructorIsIgnored() {
		MappingCollector c = context.mock(MappingCollector.class);
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		UnresolvedVar vr = new UnresolvedVar(pos, "Nil");
		vr.bind(LoadBuiltins.nil);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
		}});
		ma.visitUnresolvedVar(vr);
	}
	
	@Test
	public void aReferenceToAnotherFunctionIsRecorded() {
		MappingCollector c = context.mock(MappingCollector.class);
		
		FunctionName nameO = FunctionName.function(pos, pkg, "other");
		FunctionDefinition other = new FunctionDefinition(nameO, 0);
		
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		TypeBinder fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(other);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
			oneOf(c).recordDependency(other);
		}});
		ma.visitUnresolvedVar(vr);
	}
	
	@Test
	public void aReferenceToThisFunctionIsIgnored() {
		MappingCollector c = context.mock(MappingCollector.class);
		
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 1);
		MappingAnalyzer ma = new MappingAnalyzer(fn, c, dependencies);
		UnresolvedVar vr = new UnresolvedVar(pos, "x");
		vr.bind(fn);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		ma.visitFunctionIntro(fi);
		
		context.checking(new Expectations() {{
		}});
		ma.visitUnresolvedVar(vr);
	}
}
