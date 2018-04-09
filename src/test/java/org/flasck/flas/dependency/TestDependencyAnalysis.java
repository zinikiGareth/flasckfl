package org.flasck.flas.dependency;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.stories.FLASStory;
import org.junit.Test;

public class TestDependencyAnalysis {
	private final ErrorResult errors = new ErrorResult();
	private final DependencyAnalyzer analyzer = new DependencyAnalyzer();
	private final Rewriter rewriter = new Rewriter(errors, null, null, null);

	@Test
	public void testMutualFandG() throws ErrorResultException {
		// In spite of my desire for "chain-of-custody" testing, defining functions as cases has eluded me
		// Can we go back and refactor this at some point into the checked output of FlasStoryTests?
		Scope s = Scope.topScope("ME");
		ErrorResult er = new ErrorResult();
		new FLASStory().process("ME", s, er, BlockTestData.simpleMutualRecursionBlock(), false);
		if (er.hasErrors())
			throw new ErrorResultException(er);
		rewriter.rewritePackageScope(null, "ME", s);
		
		// Now begins the real test on this data
		List<Set<RWFunctionDefinition>> orchards = analyzer.analyze(rewriter.functions);
		assertNotNull(orchards);
	}
}
