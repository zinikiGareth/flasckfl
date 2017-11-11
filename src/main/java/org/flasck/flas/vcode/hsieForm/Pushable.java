package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public interface Pushable {

	PushReturn hsie(InputPosition loc, List<VarInSource> deps);

}
