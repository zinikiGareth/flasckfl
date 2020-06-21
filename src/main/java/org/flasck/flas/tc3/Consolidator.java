package org.flasck.flas.tc3;

import java.util.Collection;

import org.flasck.flas.blockForm.InputPosition;

public interface Consolidator {
	PosType consolidate(InputPosition pos, Collection<PosType> types);
	PosType collapse(InputPosition pos, Collection<PosType> types);
}
