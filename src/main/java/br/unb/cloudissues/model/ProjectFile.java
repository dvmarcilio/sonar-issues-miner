package br.unb.cloudissues.model;

import java.util.Map;
import java.util.Objects;

public class ProjectFile {

	private final String sonarPath;

	private final Map<String, String> metrics;
	
	public ProjectFile(String sonarPath, Map<String, String> metrics) {
		super();
		this.sonarPath = sonarPath;
		this.metrics = metrics;
	}

	public String getSonarPath() {
		return sonarPath;
	}

	public Map<String, String> getMetrics() {
		return metrics;
	}

	@Override
	public String toString() {
		return "ProjectFile [sonarPath=" + sonarPath + ", metrics=" + metrics + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(sonarPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProjectFile other = (ProjectFile) obj;
		return Objects.equals(sonarPath, other.sonarPath);
	}

}
