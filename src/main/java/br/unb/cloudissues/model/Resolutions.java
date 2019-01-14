package br.unb.cloudissues.model;

public enum Resolutions {

	FALSE_POSITIVE("FALSE-POSITIVE"), WONTFIX("WONTFIX"), FIXED("FIXED"), REMOVED("REMOVED");

	private final String value;

	Resolutions(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
