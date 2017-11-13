package org.flasck.flas.generators;

import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface ObjectReferenceGenerator<T> {

	void generate(ObjectReference defn, ObjectNeeded myOn, OutputHandler<T> handler, ClosureGenerator closure);

	void push(ObjectReference or, OutputHandler<T> handler);

}
