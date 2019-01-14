package br.unb.cloudissues.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.internal.LinkedTreeMap;

import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.ProjectLink;
import br.unb.cloudissues.util.Utils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProjectsLinksRetriever {

	private static final String PROJECT_LINKS_SEARCH_URL = "/api/project_links/search";

	private static final int DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS = 5000;
	private static final int DEFAULT_MAX_REQUESTS_TO_WAIT = 20;

	private static int count = 1;

	private final List<Project> projects;

	private final String baseUrl;

	private final int timeToWaitInMiliSecondsBetweenRequests;
	private final int maxRequestsToWait;

	private final OkHttpClient httpClient = new OkHttpClient();

	private ProjectsLinksRetriever(Builder builder) {
		projects = builder.projects;
		baseUrl = builder.baseUrl;
		timeToWaitInMiliSecondsBetweenRequests = Optional
				.ofNullable(builder.timeToWaitInMiliSecondsBetweenRequests)
				.orElse(DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS);
		maxRequestsToWait = Optional.ofNullable(builder.maxRequestsToWait)
				.orElse(DEFAULT_MAX_REQUESTS_TO_WAIT);
	}

	public static class Builder {
		private final List<Project> projects;
		private final String baseUrl;
		private Integer timeToWaitInMiliSecondsBetweenRequests;
		private Integer maxRequestsToWait;

		/**
		 * @param baseUrl Sonar Web API Base URL until /api
		 */
		public Builder(List<Project> projects, String baseUrl) {
			this.projects = projects;
			this.baseUrl = baseUrl;
		}

		public Builder timeToWaitInMiliSecondsBetweenRequests(int time) {
			timeToWaitInMiliSecondsBetweenRequests = time;
			return this;
		}

		public Builder maxRequestsToWait(int maxRequestsToWait) {
			this.maxRequestsToWait = maxRequestsToWait;
			return this;
		}

		public ProjectsLinksRetriever build() {
			return new ProjectsLinksRetriever(this);
		}
	}

	/**
	 * 
	 * @return the same List<Project> each with its List<ProjectLink> (if present)
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public List<Project> retrieve() throws IOException, InterruptedException {
		validateNeededParameters();

		count = 1;

		System.out.println("\nretrieving links for projects in " + baseUrl);
		return httpRequestLinksAndAddToProjects();
	}

	private void validateNeededParameters() {
		Objects.requireNonNull(projects);
		Objects.requireNonNull(baseUrl);
		if (projects.isEmpty()) {
			throw new IllegalArgumentException("projects should not be empty");
		}
	}

	private List<Project> httpRequestLinksAndAddToProjects()
			throws IOException, InterruptedException {
		List<Project> projectsWithLinks = new ArrayList<>(projects.size());
		for (Project project : projects) {
			Project pWithLinks = new Project(project);
			pWithLinks.setLinks(retrieveProjectLinks(project));
			projectsWithLinks.add(pWithLinks);
		}
		return projectsWithLinks;
	}

	private List<ProjectLink> retrieveProjectLinks(Project project)
			throws InterruptedException, IOException {
		String responseBody = retrieveResponseBodyForURL(buildURL(project.getProjectKey()));
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		return retrieveProjectLinksFromResponseMap(responseMap);
	}

	private String buildURL(String projectKey) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + PROJECT_LINKS_SEARCH_URL).newBuilder();
		urlBuilder.addQueryParameter("projectKey", projectKey);
		return urlBuilder.build().toString();
	}

	private String retrieveResponseBodyForURL(String url) throws InterruptedException, IOException {
		tryNotToFloodSonarWithLotsOfRequests();
		return doRetrieveResponseBodyForURL(url);
	}

	private void tryNotToFloodSonarWithLotsOfRequests() throws InterruptedException {
		if (count % maxRequestsToWait == 0) {
			System.out.println("\nWaiting for "
					+ (timeToWaitInMiliSecondsBetweenRequests / 1000.0 + " seconds..."));
			System.out.println(count + " requests so far.");
			Thread.sleep(timeToWaitInMiliSecondsBetweenRequests);
		}
		count++;
	}

	private String doRetrieveResponseBodyForURL(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();
		Response response = httpClient.newCall(request).execute();
		return response.body().string();
	}

	@SuppressWarnings("unchecked")
	private List<ProjectLink> retrieveProjectLinksFromResponseMap(Map<String, Object> responseMap) {
		List<LinkedTreeMap<String, Object>> links = (List<LinkedTreeMap<String, Object>>) responseMap
				.getOrDefault("links", Collections.emptyList());

		List<ProjectLink> projectLinks = new ArrayList<>(links.size());
		links.forEach(linkMap -> {
			ProjectLink pl = new ProjectLink();
			pl.setId((String) linkMap.get("id"));
			pl.setType((String) linkMap.get("type"));
			pl.setUrl((String) linkMap.get("url"));
			projectLinks.add(pl);
		});
		return projectLinks;
	}

}
