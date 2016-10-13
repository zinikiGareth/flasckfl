package org.flasck.flas.commonBase.android;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class AndroidLabel implements Serializable {
	public final String label;

	public AndroidLabel(InputPosition location, String label) {
		this.label = label;
	}
}
