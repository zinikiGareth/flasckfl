package org.flasck.flas.assembler;

import java.io.IOException;
import java.io.Writer;

import org.ziniki.deployment.concepts.WaitAMo;

public class FLASAssembler extends FlasckAssembler {
	private final Writer fw;

	public FLASAssembler(Writer fw, String stdlib) {
		super(stdlib);
		this.fw = fw;
	}

	@Override
	protected WaitAMo writeHTML(String html) {
		try {
			fw.write(html);
		} catch (IOException e) {
			logger.error(e.toString());
			return WaitAMo.ABORT;
		}
		return WaitAMo.CONTINUE;
	}

	@Override
	public void done() {
		try {
			fw.close();
		} catch (IOException e) {
			logger.error(e.toString());
		}
	}

}
