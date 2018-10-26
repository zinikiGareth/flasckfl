package org.flasck.flas.blocker;

import org.flasck.flas.blockForm.ContinuedLine;

public interface BlockConsumer {
	void newFile();
	// TODO: this may need some "line number" or "InputPosition" or something
	void comment(String text);
	void line(int depth, ContinuedLine currline);
}
