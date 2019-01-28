package br.unb.cloudissues.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProjectFiles {

	private final Project project;

	private final Map<String, String> metrics;

	private Set<ProjectFile> files;

	public ProjectFiles(Project project, Map<String, String> metrics) {
		super();
		this.project = project;
		this.metrics = new HashMap<>(metrics);
	}

	public Project getProject() {
		return project;
	}

	public Map<String, String> getMetrics() {
		return Collections.unmodifiableMap(metrics);
	}

	public Set<ProjectFile> getFiles() {
		return Collections.unmodifiableSet(files);
	}

	public void setFiles(Set<ProjectFile> files) {
		this.files = new HashSet<>(files);
	}

	@Override
	public String toString() {
		return "ProjectFiles [project=" + project + ", metrics=" + metrics + ", files=" + files + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(project);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProjectFiles other = (ProjectFiles) obj;
		return Objects.equals(project, other.project);
	}

}
