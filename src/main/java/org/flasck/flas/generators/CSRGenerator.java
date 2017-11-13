package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushCSR;

public interface CSRGenerator<T> {
	void generate(PushCSR csr, OutputHandler<T> handler, ClosureGenerator closure);
	void push(PushCSR csr, OutputHandler<T> handler);

}
