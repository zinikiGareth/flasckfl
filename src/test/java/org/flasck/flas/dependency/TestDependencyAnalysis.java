package org.flasck.flas.dependency;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.Rewriter;
import org.flasck.flas.depedencies.DependencyAnalyzer;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.stories.FLASStory;
import org.junit.Test;
import org.zinutils.graphs.Orchard;

public class TestDependencyAnalysis {
	private final ErrorResult errors = new ErrorResult();
	private final DependencyAnalyzer analyzer = new DependencyAnalyzer(errors);
	private final Rewriter rewriter = new Rewriter();

	@Test
	public void testMutualFandG() {
		// In spite of my desire for "chain-of-custody" testing, defining functions as cases has eluded me
		// Can we go back and refactor this at some point into the checked output of FlasStoryTests?
		Object o = new FLASStory().process("ME", BlockTestData.simpleMutualRecursionBlock());
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = (Scope) o;
		s = rewriter.rewrite(s);
		
		// Now begins the real test on this data
		List<Orchard<FunctionDefinition>> orchards = analyzer.analyze(s);
		assertNotNull(orchards);
	}

}
