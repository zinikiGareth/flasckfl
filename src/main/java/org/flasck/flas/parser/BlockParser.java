package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.tokenizers.Tokenizable;

@Deprecated
public class BlockParser {
	private final List<Class<? extends TryParsing>> choices = new ArrayList<Class<? extends TryParsing>>();
	
	@SafeVarargs
	public BlockParser(Class<? extends TryParsing>... kls) {
		for (Class<? extends TryParsing> c : kls) {
			choices.add(c);
		}
	}

	public Object parse(Block b) {
		for (Class<? extends TryParsing> k : choices) {
			try {
				TryParsing tp = k.newInstance();
				Object o = tp.tryParsing(new Tokenizable(b.line.text()));
				if (o != null)
					return o;
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

}
