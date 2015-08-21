package org.flasck.flas.dependency;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.junit.Test;
import org.zinutils.graphs.Orchard;

public class TestDependencyAnalysis {
	private final ErrorResult errors = new ErrorResult();
	private final DependencyAnalyzer analyzer = new DependencyAnalyzer(errors);
	private final Rewriter rewriter = new Rewriter(errors, null);
	private final ScopeEntry se = new PackageDefn(Builtin.builtinScope(), "ME").myEntry();

	@Test
	public void testMutualFandG() {
		// In spite of my desire for "chain-of-custody" testing, defining functions as cases has eluded me
		// Can we go back and refactor this at some point into the checked output of FlasStoryTests?
		Object o = new FLASStory().process(se, BlockTestData.simpleMutualRecursionBlock());
		assertNotNull(o);
		assertTrue(o instanceof ScopeEntry);
		ScopeEntry se = (ScopeEntry) o;
		rewriter.rewrite(se);
		
		// Now begins the real test on this data
		List<Orchard<FunctionDefinition>> orchards = analyzer.analyze(rewriter.functions);
		assertNotNull(orchards);
	}

}
