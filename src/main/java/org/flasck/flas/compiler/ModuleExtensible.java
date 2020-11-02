package org.flasck.flas.compiler;

import org.flasck.flas.repository.RepositoryVisitor;

public interface ModuleExtensible {

	<T extends TraversalProcessor> T forModule(Class<T> clz, Class<? extends RepositoryVisitor> phase);

}
