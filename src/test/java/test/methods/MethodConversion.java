package test.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.method.MessageConvertor;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class MethodConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void weDelegateToFullMethodConvertorOnVisitObjectMethod() {
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ConvertRepositoryMethods.class)));
			oneOf(nv).push(with(any(MethodConvertor.class)));
		}});
		ConvertRepositoryMethods mc = new ConvertRepositoryMethods(nv, errors, repository);
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "meth"), new ArrayList<>(), null, null);
		mc.visitObjectMethod(om);
	}

	@Test
	public void visitMessagePushesAMessageConvertor() {
		Expr e1 = context.mock(Expr.class, "e1");
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(MessageConvertor.class)));
		}});
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "meth"), new ArrayList<>(), null, null);
		MethodConvertor mc = new MethodConvertor(null, nv, om);
		mc.visitMessage(new SendMessage(pos, e1));
	}
	
	@Test
	public void leaveObjectMethodAssemblesTheMessagesInAListAndBindsToTheMethod() {
		Expr e1 = context.mock(Expr.class, "e1");
		Expr e2 = context.mock(Expr.class, "e2");
		context.checking(new Expectations() {{
			oneOf(nv).result(null);
		}});
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "meth"), new ArrayList<>(), null, null);
		MethodConvertor mc = new MethodConvertor(null, nv, om);
		mc.result(e1);
		mc.result(e2);
		mc.leaveObjectMethod(om);
		assertTrue(om.isConverted());
		List<FunctionIntro> fis = om.converted();
		assertNotNull(fis);
		assertEquals(1, fis.size());
		Messages msgs = (Messages) fis.get(0).cases().get(0).expr;
		assertEquals(2, msgs.exprs.size());
		assertEquals(e1, msgs.exprs.get(0));
		assertEquals(e2, msgs.exprs.get(1));
	}

}
