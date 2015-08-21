package org.flasck.flas.errors;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.utils.Justification;

public class ErrorResult {
	public final List<FLASError> errors = new ArrayList<FLASError>();

	public ErrorResult message(Block b, String msg) {
		return message(new Tokenizable(b), msg);
	}
	
	public ErrorResult message(Tokenizable line, String msg) {
		return message(line.realinfo(), msg);
	}

	public ErrorResult message(InputPosition pos, String msg) {
		errors.add(new FLASError(pos, msg));
		return this;
	}

	public void merge(ErrorResult from) {
		errors.addAll(from.errors);
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public void showTo(Writer pw, int ind) throws IOException {
		for (int i=0;i<ind-2;i++)
			pw.append(' ');
		pw.write(errors.size() + " error" + (errors.size() != 1?"s":"") + " encountered\n");
		for (FLASError e : errors) {
			for (int i=0;i<ind;i++)
				pw.append(' ');
			if (e.loc != null) {
				pw.write(Justification.PADRIGHT.format(e.loc + ": ", 22) + e.loc.text.substring(0, e.loc.off) + " _ " + e.loc.text.substring(e.loc.off));
				pw.write('\n');
			}
			for (int i=0;i<ind;i++)
				pw.append(' ');
			pw.write(e.msg);
			pw.write('\n');
		}
		pw.flush();
	}

	public static ErrorResult oneMessage(Tokenizable line, String msg) {
		return new ErrorResult().message(line, msg);
	}

	public static ErrorResult oneMessage(InputPosition location, String msg) {
		return new ErrorResult().message(location, msg);
	}

}
