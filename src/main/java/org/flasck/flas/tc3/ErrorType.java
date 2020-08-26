package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.NotImplementedException;

public class ErrorType implements Type {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");

	public ErrorType() {
		try {
			throw new RuntimeException("ErrorType Created");
		} catch (Exception ex) {
			logger.info("Error in TypeChecking", ex);
		}
	}
	
	@Override
	public String signature() {
		return "ERROR";
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return false;
	}

	@Override
	public String toString() {
		return "<<ERROR>>";
	}
}
