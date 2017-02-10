package test.droidgen;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;

public class GenTestsForContractImpl {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());
	ByteCodeSink bccImpl = context.mock(ByteCodeSink.class, "implClass");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bccImpl).generateAssociatedSourceFile();
			allowing(bccImpl).getCreatedName(); will(returnValue("Card"));
			allowing(expr);
			allowing(ctor).nextLocal(); will(returnValue(1));
		}});
	}

	@Test
	public void testVisitingAnEmptyStructDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfNestedClass();
		checkCreationOfImplCtor();
		RWContractImplements ci = new RWContractImplements(loc, loc, new CSName(new CardName(null, "Card"), "_C0"), new SolidName(null, "CtrDecl"), null, null);
		gen.visitContractImpl(ci);
	}

	public void checkCreationOfNestedClass() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Card$_C0"); will(returnValue(bccImpl));
			oneOf(bccImpl).superclass("CtrDecl$Impl");
			oneOf(bccImpl).defineField(false, Access.PRIVATE, new JavaType("Card"), "_card");
			oneOf(bccImpl).addInnerClassReference(Access.PUBLICSTATIC, "Card", "_C0");
		}});
	}

	public void checkCreationOfImplCtor() {
		context.checking(new Expectations() {{
			oneOf(bccImpl).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument("Card", "card"); will(new ReturnNewVar(ctor, "Card", "card"));
			oneOf(ctor).callSuper("void", "CtrDecl", "<init>"); will(returnValue(expr));
			oneOf(ctor).assign(with(any(IExpr.class)), with(any(IExpr.class)));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}
}
