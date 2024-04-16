package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;

public interface MatchGeneratorModule {

	boolean generateMatch(JSExpr runner, JSBlockCreator block, JSExpr arg, UnitTestMatch m, String freeText);

}
