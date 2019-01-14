package br.unb.cloudissues.model;

import java.util.Objects;

public class SimilarRule implements Comparable<SimilarRule> {

	private final String ruleKey;

	private final String ruleName;

	private final Float score;

	public SimilarRule(String ruleKey, String ruleName, Float score) {
		super();
		this.ruleKey = ruleKey;
		this.ruleName = ruleName;
		this.score = score;
	}

	@Override
	public int compareTo(SimilarRule o) {
		return o.score.compareTo(score);
	}

	public String getRuleKey() {
		return ruleKey;
	}

	public String getRuleName() {
		return ruleName;
	}

	public Float getScore() {
		return score;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ruleKey, ruleName, score);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimilarRule other = (SimilarRule) obj;
		return Objects.equals(ruleKey, other.ruleKey) && Objects.equals(ruleName, other.ruleName)
				&& Double.doubleToLongBits(score) == Double.doubleToLongBits(other.score);
	}

	@Override
	public String toString() {
		return "SimilarRule [ruleKey=" + ruleKey + ", ruleName=" + ruleName + ", score=" + score
				+ "]";
	}

}
