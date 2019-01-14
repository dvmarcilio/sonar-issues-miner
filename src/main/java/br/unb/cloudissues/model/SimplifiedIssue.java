package br.unb.cloudissues.model;

import java.util.Objects;

public class SimplifiedIssue {

	private final String issueKey;

	private final String component;

	private final String ruleKey;

	private final String ruleName;

	public SimplifiedIssue(String issueKey, String component, String ruleKey, String ruleName) {
		super();
		this.issueKey = issueKey;
		this.component = component;
		this.ruleKey = ruleKey;
		this.ruleName = ruleName;
	}

	public String getIssueKey() {
		return issueKey;
	}

	public String getComponent() {
		return component;
	}

	public String getRuleKey() {
		return ruleKey;
	}

	public String getRuleName() {
		return ruleName;
	}

	@Override
	public String toString() {
		return "SimplifiedIssue [issueKey=" + issueKey + ", component=" + component + ", ruleKey="
				+ ruleKey + ", ruleName=" + ruleName + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(issueKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimplifiedIssue other = (SimplifiedIssue) obj;
		return Objects.equals(issueKey, other.issueKey);
	}

}
