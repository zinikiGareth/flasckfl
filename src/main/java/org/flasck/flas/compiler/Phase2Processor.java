package org.flasck.flas.compiler;

import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public interface Phase2Processor extends TopLevelDefinitionConsumer {

	void process();

}
