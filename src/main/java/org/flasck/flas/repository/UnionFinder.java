package org.flasck.flas.repository;

import java.util.Set;

import org.flasck.flas.tc3.Type;

public interface UnionFinder {
	Type findUnionWith(Set<Type> ms);
}
