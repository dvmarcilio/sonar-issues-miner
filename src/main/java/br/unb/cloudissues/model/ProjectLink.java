package br.unb.cloudissues.model;

public class ProjectLink {

	public static final String SCM_TYPE = "scm";
	public static final String GITHUB_TYPE = SCM_TYPE;
	public static final String ISSUE_TYPE = "issue";
	public static final String CI_TYPE = "ci";
	public static final String HOMEPAGE_TYPE = "homepage";

	public static final String GITHUB_HTTPS = "https://github.com/";
	public static final String GITHUB_SSH = "git@github.com";

	private String id;

	private String type;

	private String url;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isGitHub() {
		if (type == null || url == null) {
			return false;
		}
		return type.equals(GITHUB_TYPE) && (url.contains(GITHUB_HTTPS) || url.contains(GITHUB_SSH));
	}

	public boolean isSCM() {
		return SCM_TYPE.equals(type);
	}

	public boolean isHomePage() {
		return HOMEPAGE_TYPE.equals(type);
	}

	public boolean isIssue() {
		return ISSUE_TYPE.equals(type);
	}

	public boolean isCI() {
		return CI_TYPE.equals(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		ProjectLink other = (ProjectLink) obj;
		if (type != other.type)
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProjectLink [type=" + type + ", url=" + url + "]";
	}

}
