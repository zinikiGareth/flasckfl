package test.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.method.AccessorConvertor;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.method.MessageConvertor;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class AccessorConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void weDelegateToAccessorConvertorOnVisitFunction() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv, errors, repository);
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "meth"), 4, null);
		mc.visitFunction(fn);
	}

	@Test
	public void weDelegateToAccessorConvertorOnVisitUnitTestAssert() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv, errors, repository);
		UnitTestAssert e = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "hello"));
		mc.visitUnitTestAssert(e);
	}

	@Test
	public void weDelegateToMessageConvertorOnVisitUnitTestInvoke() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(MessageConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv, errors, repository);
		UnitTestInvoke e = new UnitTestInvoke(new StringLiteral(pos, "hello"));
		mc.visitUnitTestInvoke(e);
	}

	@Test
	public void weCanConvertASimpleFieldAccessorOnATypedPattern() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		AccessorConvertor ac = new AccessorConvertor(nv, errors, repository, null);
		SolidName on = new SolidName(pkg, "ObjDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, on, true, new ArrayList<>());
		FunctionName an = FunctionName.function(pos, on, "acor");
		FunctionDefinition fn = new FunctionDefinition(an, 0, null);
		ObjectAccessor acor = new ObjectAccessor(od, fn);
		od.addAccessor(acor);
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		TypedPattern tp = new TypedPattern(pos, tr, new VarName(pos, acor.name(), "obj"));
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(tp);
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "acor"));
		boolean done = ac.visitMemberExpr(expr, 0);
		ac.leaveMemberExpr(expr, done);
		assertTrue(expr.isConverted());
		Expr conv = expr.converted();
		assertNotNull(conv);
		assertTrue(conv instanceof MakeAcor);
		MakeAcor ma = (MakeAcor) conv;
		assertEquals(0, ma.nargs);
		assertEquals(an, ma.acorMeth);
		assertEquals(from, ma.obj);
	}

	@Test
	public void weCanConvertASimpleFieldAccessorOnAUDD() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		AccessorConvertor ac = new AccessorConvertor(nv, errors, repository, null);
		SolidName on = new SolidName(pkg, "ObjDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, on, true, new ArrayList<>());
		FunctionName an = FunctionName.function(pos, on, "acor");
		FunctionDefinition fn = new FunctionDefinition(an, 0, null);
		ObjectAccessor acor = new ObjectAccessor(od, fn);
		od.addAccessor(acor);
		context.checking(new Expectations() {{
			oneOf(repository).get("test.repo.ObjDefn.acor"); will(returnValue(acor));
		}});
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "udd"), null);
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(udd);
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "acor"));
		boolean done = ac.visitMemberExpr(expr, 0);
		ac.leaveMemberExpr(expr, done);
		assertTrue(expr.isConverted());
		Expr conv = expr.converted();
		assertNotNull(conv);
		assertTrue(conv instanceof MakeAcor);
		MakeAcor ma = (MakeAcor) conv;
		assertEquals(0, ma.nargs);
		assertEquals(an, ma.acorMeth);
		assertEquals(from, ma.obj);
	}

	@Test
	public void weCanConvertASimpleFieldAccessorOnAConstantAssumingItsAStruct() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		AccessorConvertor ac = new AccessorConvertor(nv, errors, repository, null);
		FunctionName an = FunctionName.function(pos, pkg, "f");
		FunctionDefinition fn = new FunctionDefinition(an, 0, null);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Struct"), true, new ArrayList<>());
		StructField sf = new StructField(pos, sd, true, true, LoadBuiltins.stringTR, "fld");
		sf.fullName(new VarName(pos, sd.name(), "x"));
		sd.addField(sf);
		fn.bindType(sd);
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(fn);
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "fld"));
		boolean done = ac.visitMemberExpr(expr, 0);
		ac.leaveMemberExpr(expr, done);
		assertTrue(expr.isConverted());
		Expr conv = expr.converted();
		assertNotNull(conv);
		assertTrue(conv instanceof MakeAcor);
		MakeAcor ma = (MakeAcor) conv;
		assertEquals(0, ma.nargs);
		assertEquals("test.repo.Struct._field_fld", ma.acorMeth.uniqueName());
		assertEquals(from, ma.obj);
	}

	@Test
	public void anAccessorMayNeedArgs() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		AccessorConvertor ac = new AccessorConvertor(nv, errors, repository, null);
		SolidName on = new SolidName(pkg, "ObjDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, on, true, new ArrayList<>());
		FunctionName an = FunctionName.function(pos, on, "acor");
		FunctionDefinition fn = new FunctionDefinition(an, 3, null);
		ObjectAccessor acor = new ObjectAccessor(od, fn);
		od.addAccessor(acor);
		context.checking(new Expectations() {{
			oneOf(repository).get("test.repo.ObjDefn.acor"); will(returnValue(acor));
		}});
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "udd"), null);
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(udd);
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "acor"));
		boolean done = ac.visitMemberExpr(expr, 0);
		ac.leaveMemberExpr(expr, done);
		assertTrue(expr.isConverted());
		Expr conv = expr.converted();
		assertNotNull(conv);
		assertTrue(conv instanceof MakeAcor);
		MakeAcor ma = (MakeAcor) conv;
		assertEquals(3, ma.nargs);
		assertEquals(an, ma.acorMeth);
		assertEquals(from, ma.obj);
	}
	
	@Test
	public void itsAnErrorForTheAccessorToNotHaveBeenDefined() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		AccessorConvertor ac = new AccessorConvertor(nv, errors, repository, null);
		SolidName on = new SolidName(pkg, "ObjDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, on, true, new ArrayList<>());
		FunctionName an = FunctionName.function(pos, on, "acor");
		FunctionDefinition fn = new FunctionDefinition(an, 0, null);
		ObjectAccessor acor = new ObjectAccessor(od, fn);
		od.addAccessor(acor);
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "udd"), null);
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(udd);
		
		context.checking(new Expectations() {{
			oneOf(repository).get("test.repo.ObjDefn.notThere"); will(returnValue(null));
			oneOf(errors).message(pos, "there is no suitable value for 'notThere' on test.repo.ObjDefn");
		}});
		
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "notThere"));
		boolean done = ac.visitMemberExpr(expr, 0);
		ac.leaveMemberExpr(expr, done);
		assertFalse(expr.isConverted());
	}
}
