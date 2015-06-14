package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;

public interface StoryProcessor {
	Object process(ScopeEntry top, List<Block> b);
}
