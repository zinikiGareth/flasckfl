package org.flasck.flas;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.tokenizers.Tokenizable;

public class ErrorResult {
	private final List<FLASError> errors = new ArrayList<FLASError>();

	public ErrorResult message(Tokenizable line, String msg) {
		errors.add(new FLASError(line.realinfo(), msg));
		return this;
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public static ErrorResult oneMessage(Tokenizable line, String msg) {
		return  new ErrorResult().message(line, msg);
	}

}
