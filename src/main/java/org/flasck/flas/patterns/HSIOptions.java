package org.flasck.flas.patterns;

public interface HSIOptions {
	void addCM(String ctor, HSITree nested);
	HSITree getCM(String constructor);
}
