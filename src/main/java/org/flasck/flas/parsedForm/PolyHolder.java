package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.tc3.NamedType;

public interface PolyHolder extends NamedType {
	boolean hasPolys();
	List<PolyType> polys();
}
