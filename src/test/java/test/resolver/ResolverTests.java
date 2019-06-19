package test.resolver;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ResolverTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
//	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final FunctionName namePlPl = FunctionName.function(pos, null, "++");

	@Test
	public void testWeCanResolveASimpleName() {
		Repository ry = new Repository();
		final FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		ry.functionDefn(fn);
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCanResolveASimpleOperator() {
		Repository ry = new Repository();
		final FunctionDefinition fn = new FunctionDefinition(namePlPl, 2);
		ry.functionDefn(fn);
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "++");
		r.visitUnresolvedOperator(var);
		assertEquals(fn, var.defn());
	}

	@Test
	public void anUndefinedNameCantBeResolved() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot resolve 'f'");
		}});
		Repository ry = new Repository();
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var);
	}

}
