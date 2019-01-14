package br.unb.cloudissues.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class Project {

	private String id;

	private String organization;

	private String projectName;

	private String projectKey;

	private List<String> tags;

	private boolean isGitHub;

	private List<ProjectLink> links;

	private transient String normalizedGithubUrl;

	public Project() {
	}

	public Project(Project project) {
		setId(project.id);
		setOrganization(project.organization);
		setProjectName(project.projectName);
		setProjectKey(project.projectKey);
		setTags(new ArrayList<String>(
				Optional.ofNullable(project.tags).orElse(Collections.emptyList())));
		setLinks(new ArrayList<ProjectLink>(
				Optional.ofNullable(project.links).orElse(Collections.emptyList())));
	}

	public String getNormalizedGithubURL() {
		if (normalizedGithubUrl == null) {
			normalizedGithubUrl = doGetNormalizedGithubURL();
		}
		return normalizedGithubUrl;
	}

	private String doGetNormalizedGithubURL() {
		checkGithubURLConditions();
		String gitHubURL = getGithubURL();
		int indexGhHttps = gitHubURL.indexOf(ProjectLink.GITHUB_HTTPS);
		if (indexGhHttps != -1) {
			return gitHubURL.substring(indexGhHttps);
		} else {
			return normalizeGitHubSshURL(gitHubURL);
		}
	}

	private String normalizeGitHubSshURL(String gitHubURL) {
		String ghSSH = findGhSSHForURL(gitHubURL);
		int indexGhSSH = gitHubURL.indexOf(ghSSH);
		int indexFileExt = findIndexFileExt(gitHubURL);
		return ProjectLink.GITHUB_HTTPS
				+ gitHubURL.substring(indexGhSSH + ghSSH.length(), indexFileExt);
	}

	private String findGhSSHForURL(String gitHubURL) {
		String ghSshCommon = ProjectLink.GITHUB_SSH + ":";
		if (gitHubURL.contains(ghSshCommon)) {
			return ghSshCommon;
		} else if (gitHubURL.contains(ProjectLink.GITHUB_SSH + "/")) {
			return ProjectLink.GITHUB_SSH + "/";
		}
		throw new IllegalArgumentException(
				"ssh URL does not contain known substrings: " + gitHubURL);
	}

	private int findIndexFileExt(String gitHubURL) {
		int indexFileExt = gitHubURL.indexOf(".git");
		if (indexFileExt == -1) {
			System.out.println("ssh URL does not contain known file suffix (.git): " + gitHubURL);
			System.out.println("assuming end of string as already valid");
			return gitHubURL.length();
		}
		return indexFileExt;
	}

	private void checkGithubURLConditions() {
		Objects.requireNonNull(links);
		if (!isGitHub || links.isEmpty()) {
			throw new IllegalStateException();
		}
	}

	private String getGithubURL() {
		ProjectLink pl = links.stream().filter(ProjectLink::isGitHub).findFirst()
				.orElseThrow(IllegalStateException::new);
		return pl.getUrl();
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public boolean isGitHub() {
		return isGitHub;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<ProjectLink> getLinks() {
		return links;
	}

	public void setLinks(List<ProjectLink> links) {
		this.links = links;
		isGitHub = linksHasAGithubLink();
	}

	private boolean linksHasAGithubLink() {
		if (links == null || links.isEmpty()) {
			return false;
		}
		return links.stream().filter(ProjectLink::isGitHub).findAny().isPresent();
	}

	public Optional<ProjectLink> getSCMLink() {
		return getLinkFromTypePredicate(ProjectLink::isSCM);
	}

	private Optional<ProjectLink> getLinkFromTypePredicate(Predicate<ProjectLink> predicate) {
		if (links == null || links.isEmpty()) {
			return Optional.empty();
		}
		return links.stream().filter(predicate).findFirst();
	}

	public Optional<ProjectLink> getIssueLink() {
		return getLinkFromTypePredicate(ProjectLink::isIssue);
	}

	public Optional<ProjectLink> getHomePageLink() {
		return getLinkFromTypePredicate(ProjectLink::isHomePage);
	}

	public Optional<ProjectLink> getCiLink() {
		return getLinkFromTypePredicate(ProjectLink::isCI);
	}

	@Override
	// @formatter:off
	public String toString() {
		return "Project [" +
				"\n\tid = " + id + ", " +
				"\n\torganization = " + organization + "," +
				"\n\tprojectName = " + projectName + "," +
				"\n\tprojectKey = " + projectKey + "," +
				"\ntags = " + tags + "," +
				"\nlinks = " + links + "," +
				"\n\tisGitHub = " + isGitHub +
				"\n]";
	}
	// @formatter:on

	@Override
	public int hashCode() {
		return Objects.hash(projectKey, projectName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Project other = (Project) obj;
		return Objects.equals(projectKey, other.projectKey)
				&& Objects.equals(projectName, other.projectName);
	}

}
