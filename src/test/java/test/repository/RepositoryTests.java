package test.repository;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.Repository;
import org.junit.Test;

public class RepositoryTests {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void test() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, new PackageName("test.repo"), "fred"), 2);
		r.functionDefn(fn);
		assertEquals(fn, r.get("test.repo.fred"));
	}

}
