package org.flasck.flas;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.tokenizers.Tokenizable;

public class ErrorResult {
	private final List<FLASError> errors = new ArrayList<FLASError>();

	public ErrorResult message(Tokenizable line, String msg) {
		errors.add(new FLASError(line.realinfo(), msg));
		return this;
	}

	public ErrorResult message(Block b, String msg) {
		return message(new Tokenizable(b), msg);
	}

	public void merge(ErrorResult from) {
		errors.addAll(from.errors);
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public void showTo(Writer pw) throws IOException {
		pw.write(errors.size() + " error" + (errors.size() != 1?"s":"") + " encountered\n");
		for (FLASError e : errors) {
			if (e.loc != null) {
				pw.write(e.loc + ": " + e.loc.text);
				pw.write('\n');
			}
			pw.write(e.msg);
			pw.write('\n');
		}
		pw.flush();
	}

	public static ErrorResult oneMessage(Tokenizable line, String msg) {
		return  new ErrorResult().message(line, msg);
	}

}
