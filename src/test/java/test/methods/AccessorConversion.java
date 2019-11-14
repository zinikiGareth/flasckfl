package test.methods;

import static org.junit.Assert.assertEquals;
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
import org.flasck.flas.method.AccessorConvertor;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.NestedVisitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class AccessorConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void weDelegateToAccessorConvertorOnVisitFunction() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv);
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "meth"), 4);
		mc.visitFunction(fn);
	}

	@Test
	public void weDelegateToAccessorConvertorOnVisitUnitTestAssert() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(AccessorConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv);
		UnitTestAssert e = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "hello"));
		mc.visitUnitTestAssert(e);
	}

	@Test
	public void weCanConvertASimpleFieldAccessorOnAUDD() {
		AccessorConvertor ac = new AccessorConvertor();
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "ObjDefn"), true, new ArrayList<>());
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "acor"), 0);
		ObjectAccessor acor = new ObjectAccessor(fn);
		od.addAccessor(acor);
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "udd"), null);
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		from.bind(udd);
		MemberExpr expr = new MemberExpr(pos, from, new UnresolvedVar(pos, "acor"));
		ac.visitMemberExpr(expr);
		assertTrue(expr.isConverted());
		Expr conv = expr.converted();
		assertNotNull(conv);
		assertTrue(conv instanceof MakeAcor);
		MakeAcor ma = (MakeAcor) conv;
		assertEquals(from, ma.obj);
	}
}
