package org.flasck.flas.generators;

import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;

public interface CardMemberGenerator<T> {

	void generate(CardMember cm, HSIEForm form, ObjectNeeded myOn, OutputHandler<T> handler, ClosureGenerator closure);

}
