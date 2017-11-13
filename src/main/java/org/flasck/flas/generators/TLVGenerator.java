package org.flasck.flas.generators;

import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushTLV;

public interface TLVGenerator<T> {

	void generate(PushTLV pt, OutputHandler<T> handler, ClosureGenerator closure);

}
