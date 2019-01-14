package br.unb.cloudissues.model;

import java.util.List;

/**
 * Represents the return from /api/components/search_projects
 *
 */
public class SonarProjectComponent {

	private String organization;

	private String id;

	private String key;

	private String name;

	private List<String> tags;

	public Project toProject() {
		Project p = new Project();
		p.setId(id);
		p.setProjectKey(key);
		p.setProjectName(name);
		p.setTags(tags);
		return p;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		SonarProjectComponent other = (SonarProjectComponent) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SonarProjectComponent [organization=" + organization + ", id=" + id + ", key=" + key
				+ ", name=" + name + ", tags=" + tags + "]";
	}

}
