package test.droidgen;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class GenTestsForStructs {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(null, true, bce);
	ByteCodeSink bccStruct = context.mock(ByteCodeSink.class);
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner dfe = context.mock(MethodDefiner.class, "dfe");
	
	IExpr expr = context.mock(IExpr.class);
	
	// The number of assertions in here suggests that we should break the code into three
	// parts that we could test separately
	@Test
	public void testVisitingAnEmptyStructDefnGeneratesTheCorrectMinimumCode() {
		context.checking(new Expectations() {{
			allowing(expr);
			oneOf(bce).newClass("Struct"); will(returnValue(bccStruct));
			oneOf(bccStruct).superclass(J.FLAS_OBJECT);
			oneOf(bccStruct).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).callSuper("void", J.FLAS_OBJECT, "<init>"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
			oneOf(bccStruct).createMethod(false, "void", "_doFullEval"); will(returnValue(dfe));
			oneOf(dfe).returnVoid(); will(returnValue(expr));
		}});
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Struct"), true);
		gen.visitStructDefn(sd);
	}

}
