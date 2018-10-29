package org.flasck.flas.compiler;

import org.flasck.flas.parser.TopLevelDefnConsumer;

public interface Phase2Processor extends TopLevelDefnConsumer {

	void process();

}
