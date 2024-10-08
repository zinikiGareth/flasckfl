package test.patterns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.BackupPlan;
import org.flasck.flas.repository.DontConsiderAgain;
import org.flasck.flas.repository.FunctionHSICases;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.repository.Traverser.VarMapping;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class PatternAnalysis {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, 42);
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final RepositoryVisitor v = context.mock(RepositoryVisitor.class);
	final HSIVisitor hsi = context.mock(HSIVisitor.class);
	final RepositoryReader repo = context.mock(RepositoryReader.class);

	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	final StackVisitor sv = new StackVisitor();
	final PatternAnalyzer analyzer = new PatternAnalyzer(null, repo, sv);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final FunctionIntro intro = null;

	@Test
	public void analyzeFunctionWithNoArguments() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			intro = new FunctionIntro(nameF, new ArrayList<>());
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		context.checking(new Expectations() {{
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analysisWorksForStandaloneMethodsToo() {
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "meth"), new ArrayList<>(), null, null);
		om.sendMessage(new SendMessage(pos, simpleExpr));
		StandaloneMethod meth = new StandaloneMethod(om);
		new Traverser(sv).visitStandaloneMethod(meth);

		StackVisitor mc = new StackVisitor();
		new ConvertRepositoryMethods(mc, errors, repo);
		new Traverser(mc).visitStandaloneMethod(meth);
		
		List<FunctionIntro> conv = meth.converted();
		FunctionIntro intro = conv.get(0);
		FunctionCaseDefn case1 = intro.cases().get(0);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		context.checking(new Expectations() {{
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(case1);
			oneOf(hsi).visitExpr(with(any(Messages.class)), with(0));
			oneOf(hsi).visitMessages(with(any(Messages.class)));
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).leaveMessages(with(any(Messages.class)));
			oneOf(hsi).leaveCase(case1);
			oneOf(hsi).endInline(intro);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(conv), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithASingleVar() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new VarPattern(pos, new VarName(pos, nameF, "x")));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s0);
		context.checking(new Expectations() {{
			oneOf(hsi).bind(s0, "x");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithATypedVar() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			TypeReference tr = new TypeReference(pos, "Number");
			tr.bind(LoadBuiltins.number);
			args.add(new TypedPattern(pos, tr, new VarName(pos, nameF, "x")));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(s);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Number"));
			oneOf(hsi).bind(s, "x");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void argumentsCanBeContracts() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(LoadBuiltins.builtinPkg, "Svc"));
		
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			TypeReference tr = new TypeReference(pos, "Svc");
			tr.bind(cd);
			args.add(new TypedPattern(pos, tr, new VarName(pos, nameF, "x")));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(s);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Svc"));
			oneOf(hsi).bind(s, "x");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithAConstant() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstPattern(pos, ConstPattern.INTEGER, "42"));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		context.checking(new Expectations() {{
			oneOf(repo).get("Number"); will(returnValue(LoadBuiltins.number));
		}});
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s);
		Sequence seq = context.sequence("gen");
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(s); inSequence(seq);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Number")); inSequence(seq);
			oneOf(hsi).matchNumber(42); inSequence(seq);
			oneOf(hsi).startInline(intro); inSequence(seq);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0); inSequence(seq);
			oneOf(hsi).visitNumericLiteral(number); inSequence(seq);
			oneOf(hsi).endInline(intro); inSequence(seq);
			oneOf(hsi).matchDefault(); inSequence(seq);
			oneOf(hsi).errorNoCase(); inSequence(seq);
			oneOf(hsi).defaultCase(); inSequence(seq);
			oneOf(hsi).errorNoCase(); inSequence(seq);
			oneOf(hsi).endSwitch(); inSequence(seq);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithAStringConstant() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstPattern(pos, ConstPattern.STRING, "hello"));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		context.checking(new Expectations() {{
			oneOf(repo).get("String"); will(returnValue(LoadBuiltins.string));
		}});
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s);
		Sequence seq = context.sequence("gen");
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(s); inSequence(seq);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "String")); inSequence(seq);
			oneOf(hsi).matchString("hello"); inSequence(seq);
			oneOf(hsi).startInline(intro); inSequence(seq);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0); inSequence(seq);
			oneOf(hsi).visitNumericLiteral(number); inSequence(seq);
			oneOf(hsi).endInline(intro); inSequence(seq);
			oneOf(hsi).matchDefault(); inSequence(seq);
			oneOf(hsi).errorNoCase(); inSequence(seq);
			oneOf(hsi).defaultCase(); inSequence(seq);
			oneOf(hsi).errorNoCase(); inSequence(seq);
			oneOf(hsi).endSwitch(); inSequence(seq);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithASimpleNoArgConstructor() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithAChoiceOfTwoConstructors() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro1;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro1 = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro1.functionCase(fcd1);
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		final FunctionCaseDefn fcd2;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Cons").bind(LoadBuiltins.cons));
			intro2 = new FunctionIntro(nameF, args);
			fcd2 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
			intro2.functionCase(fcd2);
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"));
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitCase(fcd2);
			oneOf(hsi).leaveCase(fcd2);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
		assertNotNull(fn.hsiTree());
	}

	@Test
	public void analyzeFunctionTwoVars() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new VarPattern(pos, new VarName(pos, nameF, "x")));
			args.add(new VarPattern(pos, new VarName(pos, nameF, "y")));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot s0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(s0);
		ArgSlot s1 = new ArgSlot(1, fn.hsiTree().get(1));
		slots.add(s1);
		context.checking(new Expectations() {{
			oneOf(hsi).bind(s0, "x");
			oneOf(hsi).bind(s1, "y");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void analyzeFunctionWithTwoNoArgConstructors() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		final FunctionIntro intro;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		ArgSlot a1 = new ArgSlot(1, fn.hsiTree().get(1));
		slots.add(a1);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).switchOn(a1);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void twoArgumentsAndTwoEquations() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		final FunctionIntro intro1;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "True").bind(LoadBuiltins.trueT));
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro1 = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro1.functionCase(fcd1);
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		final FunctionCaseDefn fcd2;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "False").bind(LoadBuiltins.falseT));
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro2 = new FunctionIntro(nameF, args);
			fcd2 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
			intro2.functionCase(fcd2);
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		ArgSlot a1 = new ArgSlot(1, fn.hsiTree().get(1));
		slots.add(a1);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "False"));
			oneOf(hsi).switchOn(a1);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));
			oneOf(hsi).switchOn(a1);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitCase(fcd2);
			oneOf(hsi).leaveCase(fcd2);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}

	@Test
	public void twoArgumentsAndTwoEquationsWithAVar() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		final FunctionIntro intro1;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "True").bind(LoadBuiltins.trueT));
			args.add(new ConstructorMatch(pos, "Nil").bind(LoadBuiltins.nil));
			intro1 = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro1.functionCase(fcd1);
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		final FunctionCaseDefn fcd2;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "False").bind(LoadBuiltins.falseT));
			args.add(new VarPattern(pos, new VarName(pos, nameF, "v")));
			intro2 = new FunctionIntro(nameF, args);
			fcd2 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
			intro2.functionCase(fcd2);
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		ArrayList<Slot> slots = new ArrayList<>();
		VarMapping vars = new VarMapping();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		ArgSlot a1 = new ArgSlot(1, fn.hsiTree().get(1));
		slots.add(a1);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "False"));
			oneOf(hsi).bind(a1, "v");
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitCase(fcd2);
			oneOf(hsi).leaveCase(fcd2);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));
			oneOf(hsi).switchOn(a1);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
	}
	
	@Test
	public void aNestedCons() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro1;
		final FunctionCaseDefn fcd1;
		{
			List<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Cons").bind(LoadBuiltins.cons));
			intro1 = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, number);
			intro1.functionCase(fcd1);
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		final FunctionCaseDefn fcd2;
		{
			List<Pattern> args = new ArrayList<>();
			ConstructorMatch cm = new ConstructorMatch(pos, "Cons").bind(LoadBuiltins.cons);
			cm.args.add(cm.new Field(pos, "head", new ConstructorMatch(pos, "True").bind(LoadBuiltins.trueT)));
			args.add(cm);
			intro2 = new FunctionIntro(nameF, args);
			fcd2 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
			intro2.functionCase(fcd2);
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		VarMapping vars = new VarMapping();
		ArrayList<Slot> slots = new ArrayList<>();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		CaptureAction cfSlot = new CaptureAction(null);
		CaptureAction switchSlot = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"));
			oneOf(hsi).constructorField(with(a0), with("head"), with(any(CMSlot.class))); will(cfSlot);
			oneOf(hsi).switchOn(with(any(CMSlot.class))); will(switchSlot);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitCase(fcd2);
			oneOf(hsi).leaveCase(fcd2);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).defaultCase();
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).endSwitch();
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
		assertNotNull(fn.hsiTree());
		assertEquals(cfSlot.get(2), switchSlot.get(0));
	}
	
	@Test
	public void aNestedConsOtherOrder() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		final FunctionIntro intro1;
		final FunctionCaseDefn fcd1;
		{
			ArrayList<Pattern> args = new ArrayList<>();
			ConstructorMatch cm = new ConstructorMatch(pos, "Cons").bind(LoadBuiltins.cons);
			cm.args.add(cm.new Field(pos, "head", new ConstructorMatch(pos, "True").bind(LoadBuiltins.trueT)));
			args.add(cm);
			intro1 = new FunctionIntro(nameF, args);
			fcd1 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
			intro1.functionCase(fcd1);
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		final FunctionCaseDefn fcd2;
		{
			ArrayList<Pattern> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Cons").bind(LoadBuiltins.cons));
			intro2 = new FunctionIntro(nameF, args);
			fcd2 = new FunctionCaseDefn(pos, intro, null, number);
			intro2.functionCase(fcd2);
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		VarMapping vars = new VarMapping();
		ArrayList<Slot> slots = new ArrayList<>();
		ArgSlot a0 = new ArgSlot(0, fn.hsiTree().get(0));
		slots.add(a0);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"));
			oneOf(hsi).constructorField(with(a0), with("head"), with(any(CMSlot.class)));
			oneOf(hsi).switchOn(with(any(CMSlot.class)));
			oneOf(hsi).withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitCase(fcd1);
			oneOf(hsi).leaveCase(fcd1);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).defaultCase();
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitCase(fcd2);
			oneOf(hsi).leaveCase(fcd2);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).endSwitch();
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		new Traverser(hsi).withHSI().visitHSI(vars, "", slots, new FunctionHSICases(fn.intros()), null, new BackupPlan(), new DontConsiderAgain());
		assertNotNull(fn.hsiTree());
	}
	
	// TODO: the errors cases where the patterns overlap
	// and there should be a golden case functions/patterns/errors
}
