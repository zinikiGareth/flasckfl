package org.flasck.flas.testrunner;

import netscape.javascript.JSObject;

public class CardHandle {
	public final JSObject handle;
	public final JSObject card;
	public final JSObject wrapper;

	public CardHandle(JSObject handle, JSObject card, JSObject wrapper) {
		this.handle = handle;
		this.card = card;
		this.wrapper = wrapper;
	}
}
