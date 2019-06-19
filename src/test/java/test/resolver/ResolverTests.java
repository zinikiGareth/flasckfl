package test.resolver;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.junit.Test;

public class ResolverTests {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final FunctionName namePlPl = FunctionName.function(pos, null, "++");

	@Test
	public void testWeCanResolveASimpleName() {
		Repository ry = new Repository();
		final FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		ry.functionDefn(fn);
		Resolver r = new RepositoryResolver(ry);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCanResolveASimpleOperator() {
		Repository ry = new Repository();
		final FunctionDefinition fn = new FunctionDefinition(namePlPl, 2);
		ry.functionDefn(fn);
		Resolver r = new RepositoryResolver(ry);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "++");
		r.visitUnresolvedOperator(var);
		assertEquals(fn, var.defn());
	}

}
