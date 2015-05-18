package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.Block;

public interface StoryProcessor {
	Object process(List<Block> b);
}
