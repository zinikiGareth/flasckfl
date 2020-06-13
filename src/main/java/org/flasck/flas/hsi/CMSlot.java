package org.flasck.flas.hsi;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIOptions;

public class CMSlot implements Slot {
	private final HSIOptions contained;
	private final String id;
	private final List<FunctionIntro> backupIntros;

	public CMSlot(String id, HSIOptions contained, List<FunctionIntro> backupIntrosThatAreFine) {
		this.id = id;
		this.contained = contained;
		this.backupIntros = backupIntrosThatAreFine != null ? backupIntrosThatAreFine : new ArrayList<FunctionIntro>();
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public HSIOptions getOptions() {
		return contained;
	}

	@Override
	public List<FunctionIntro> lessSpecific() {
		return backupIntros;
	}
	
	@Override
	public int score() {
		return contained.score();
	}

	@Override
	public String id() {
		return id;
	}
	
	@Override
	public String toString() {
		return "CMSlot[" + id + "]";
	}
}
