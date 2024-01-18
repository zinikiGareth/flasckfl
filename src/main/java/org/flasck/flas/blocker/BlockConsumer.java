package org.flasck.flas.blocker;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.InputPosition;

public interface BlockConsumer {
	void newFile();
	void comment(InputPosition location, String text);
	void line(int depth, ContinuedLine currline);
	void flush();
}
