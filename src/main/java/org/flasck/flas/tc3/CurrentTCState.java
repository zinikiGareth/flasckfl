package org.flasck.flas.tc3;

public interface CurrentTCState {
	UnifiableType hasVar(String var);
	UnifiableType functionParameter(String var);
}
