package br.unb.cloudissues.model;

import java.time.Duration;
import java.time.OffsetDateTime;

import br.unb.cloudissues.util.Utils;

public class Issue {

	private String key;
	private String rule;
	private String component;
	private TextRange textRange;
	private String resolution;
	private String status;
	private String effort;
	private String severity;
	private String type;

	private String project;
	private String subproject;

	private String creationDate;
	private String updateDate;
	private String closeDate;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public TextRange getTextRange() {
		return textRange;
	}

	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getEffort() {
		return effort;
	}

	public void setEffort(String effort) {
		this.effort = effort;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

	public String getCloseDate() {
		return closeDate;
	}

	public void setCloseDate(String closeDate) {
		this.closeDate = closeDate;
	}

	public Duration getTimeToFix() {
		return Duration.between(parseDate(getCreationDate()), parseDate(getCloseDate()));
	}

	// Parsing dates without ":" in the offset.
	private OffsetDateTime parseDate(String date) {
		return OffsetDateTime.parse(date, Utils.formatterForOffsetDateWithoutCollon());
	}

	public Long getTimeToFixAsSeconds() {
		try {
			return getTimeToFix().getSeconds();
		} catch (NullPointerException e) {
			return -1L;
		}
	}

	@Override
	public String toString() {
		return "Issue [key=" + key + ", rule=" + rule + ", component=" + component + ", textRange="
				+ textRange + ", resolution=" + resolution + ", status=" + status + ", effort="
				+ effort + ", severity=" + severity + ", type=" + type + ", project=" + project
				+ ", subproject=" + subproject + ", creationDate=" + creationDate + ", updateDate="
				+ updateDate + ", closeDate=" + closeDate + "]";
	}

	public String getSubproject() {
		return subproject;
	}

	public void setSubproject(String subproject) {
		this.subproject = subproject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((closeDate == null) ? 0 : closeDate.hashCode());
		result = prime * result + ((component == null) ? 0 : component.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((effort == null) ? 0 : effort.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((resolution == null) ? 0 : resolution.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		result = prime * result + ((severity == null) ? 0 : severity.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((textRange == null) ? 0 : textRange.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((updateDate == null) ? 0 : updateDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Issue other = (Issue) obj;
		if (closeDate == null) {
			if (other.closeDate != null)
				return false;
		} else if (!closeDate.equals(other.closeDate))
			return false;
		if (component == null) {
			if (other.component != null)
				return false;
		} else if (!component.equals(other.component))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (effort == null) {
			if (other.effort != null)
				return false;
		} else if (!effort.equals(other.effort))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (resolution == null) {
			if (other.resolution != null)
				return false;
		} else if (!resolution.equals(other.resolution))
			return false;
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		if (severity == null) {
			if (other.severity != null)
				return false;
		} else if (!severity.equals(other.severity))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (textRange == null) {
			if (other.textRange != null)
				return false;
		} else if (!textRange.equals(other.textRange))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (updateDate == null) {
			if (other.updateDate != null)
				return false;
		} else if (!updateDate.equals(other.updateDate))
			return false;
		return true;
	}

}
