package org.flasck.flas.dependency;

import static org.junit.Assert.assertNotNull;
import java.util.List;

import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.stories.FLASStory;
import org.junit.Test;
import org.zinutils.graphs.Orchard;

public class TestDependencyAnalysis {
	private final ErrorResult errors = new ErrorResult();
	private final DependencyAnalyzer analyzer = new DependencyAnalyzer(errors);
	private final Rewriter rewriter = new Rewriter(errors, null);

	@Test
	public void testMutualFandG() {
		// In spite of my desire for "chain-of-custody" testing, defining functions as cases has eluded me
		// Can we go back and refactor this at some point into the checked output of FlasStoryTests?
		Scope s = new Scope(null, null);
		new FLASStory().process(s, BlockTestData.simpleMutualRecursionBlock());
		rewriter.rewritePackageScope("ME", s);
		
		// Now begins the real test on this data
		List<Orchard<RWFunctionDefinition>> orchards = analyzer.analyze(rewriter.functions);
		assertNotNull(orchards);
	}

}
