package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;

public interface ExprGeneratorModule {

	boolean generateFnOrCtor(NestedVisitor sv, JSFunctionState state, JSBlockCreator block, RepositoryEntry defn, String myName, int nargs);

}
