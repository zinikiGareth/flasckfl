package org.flasck.flas.repository;

import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tc3.Type;

public interface UnionFinder {
	Type findUnionWith(ErrorReporter errors, InputPosition pos, Set<Type> ms, boolean needAll);
}
