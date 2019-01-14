package br.unb.cloudissues.model;

public class Rule {

	private String key;
	private String repo;
	private String name;
	private String severity;
	private String status;
	private String type;
	private String lang;
	private String lagName;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getLagName() {
		return lagName;
	}

	public void setLagName(String lagName) {
		this.lagName = lagName;
	}

	@Override
	public String toString() {
		return "Rule [key=" + key + ", repo=" + repo + ", name=" + name + ", severity=" + severity + ", status="
				+ status + ", type=" + type + ", lang=" + lang + ", lagName=" + lagName + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		Rule other = (Rule) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

}
