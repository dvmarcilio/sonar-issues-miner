package br.unb.cloudissues.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public class Violations {

	public static final UnaryOperator<String> removeProjectKeyFromFilePath = path -> {
		int projectKeyPrefixIndex = path.lastIndexOf(':') + 1;
		return path.substring(projectKeyPrefixIndex);
	};

	private Project project;

	private Long total;

	private List<Issue> issues;

	public Violations(Project project, Long total, List<Issue> issues) {
		super();
		this.project = project;
		this.total = total;
		this.issues = new ArrayList<>(issues);
	}

	public Violations(Project project, String total, List<Issue> issues) {
		this(project, Long.parseLong(total), issues);
	}

	public Set<String> getFilesPaths() {
		throw new IllegalStateException("Not implemented in this version");
	}

	public Project getProject() {
		return project;
	}

	public Long getTotal() {
		return total;
	}

	public List<Issue> getIssues() {
		return issues;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public void setIssues(List<Issue> issues) {
		this.issues = issues;
	}

	public String getProjectKey() {
		return project.getProjectKey();
	}

	@Override
	public String toString() {
		return "Violations [project=" + project + ", total=" + total + ", issues=" + issues + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((project == null) ? 0 : project.hashCode());
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
		Violations other = (Violations) obj;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project)) {
			return false;
		}
		return true;
	}

}
