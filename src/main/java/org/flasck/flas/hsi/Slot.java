package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;

public interface Slot {

	HSIOptions getOptions();
	int score();
	String id();

}
