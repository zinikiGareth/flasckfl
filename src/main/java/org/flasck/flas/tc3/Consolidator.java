package org.flasck.flas.tc3;

import java.util.Collection;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public interface Consolidator {
	PosType consolidate(InputPosition location, List<PosType> results);
	PosType collapse(InputPosition pos, Collection<PosType> types);
}
