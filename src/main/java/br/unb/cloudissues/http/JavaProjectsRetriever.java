package br.unb.cloudissues.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.internal.LinkedTreeMap;

import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.SonarProjectComponent;
import br.unb.cloudissues.util.Utils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JavaProjectsRetriever {

	private static final Integer DEFAULT_PAGE_SIZE = 500;

	// SonarCloud is not returning any value after the 10_000th
	private static final Integer MAX_RESULTS_THAT_SONAR_RETURN = 10_000;

	private static final int DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS = 6500;
	private static final int DEFAULT_MAX_REQUESTS_TO_WAIT = 10;

	private static final String PROJECTS_SEARCH_URL = "/components/search_projects";

	private static int count = 1;

	private final boolean isSonarCloud;

	private final String baseUrl;

	private final int timeToWaitInMiliSecondsBetweenRequests;
	private final int maxRequestsToWait;

	private final OkHttpClient httpClient = new OkHttpClient();

	private JavaProjectsRetriever(Builder builder) {
		baseUrl = builder.baseUrl;
		isSonarCloud = Optional.ofNullable(builder.isSonarCloud).orElse(true);
		timeToWaitInMiliSecondsBetweenRequests = Optional
				.ofNullable(builder.timeToWaitInMiliSecondsBetweenRequests)
				.orElse(DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS);
		maxRequestsToWait = Optional.ofNullable(builder.maxRequestsToWait)
				.orElse(DEFAULT_MAX_REQUESTS_TO_WAIT);
	}

	public static class Builder {

		private final String baseUrl;
		private Boolean isSonarCloud;
		private Integer timeToWaitInMiliSecondsBetweenRequests;
		private Integer maxRequestsToWait;

		/**
		 * @param baseUrl Sonar Web API Base URL until /api
		 */
		public Builder(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public Builder isSonarCloud(boolean isSonarCloud) {
			this.isSonarCloud = isSonarCloud;
			return this;
		}

		public Builder withTimeToWaitInMiliSecondsBetweenRequests(int time) {
			timeToWaitInMiliSecondsBetweenRequests = time;
			return this;
		}

		public Builder withMaxRequestsToWait(int maxRequestsToWait) {
			this.maxRequestsToWait = maxRequestsToWait;
			return this;
		}

		public JavaProjectsRetriever build() {
			return new JavaProjectsRetriever(this);
		}
	}

	public List<Project> retrieve() throws IOException, InterruptedException {
		Objects.requireNonNull(this.baseUrl);

		count = 1;

		System.out.println("\nretrieving Java projects in " + baseUrl);
		return httpRequestJavaProjects();

	}

	private List<Project> httpRequestJavaProjects() throws IOException, InterruptedException {
		String responseBody = retrieveResponseBodyForURL(buildURL());
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		@SuppressWarnings("unchecked")
		Long total = ((Map<String, Double>) responseMap.get("paging")).get("total").longValue();

		List<Project> projects = new ArrayList<>(total.intValue());

		List<SonarProjectComponent> projectsComponents = retrieveSonarProjectComponentsFromResponseMap(
				responseMap);

		projects.addAll(projectsComponents.stream().map(SonarProjectComponent::toProject)
				.collect(Collectors.toList()));

		if (total > DEFAULT_PAGE_SIZE) {
			projects.addAll(requestsProjectsForMoreThanOnePage(total));
		}

		return projects;
	}

	private String buildURL() {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + PROJECTS_SEARCH_URL).newBuilder();
		urlBuilder.addQueryParameter("filter", "languages=java");
		urlBuilder.addQueryParameter("ps", DEFAULT_PAGE_SIZE.toString());
		return urlBuilder.build().toString();
	}

	private String retrieveResponseBodyForURL(String url) throws IOException, InterruptedException {
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
	private List<SonarProjectComponent> retrieveSonarProjectComponentsFromResponseMap(
			Map<String, Object> responseMap) {
		List<LinkedTreeMap<String, Object>> components = (List<LinkedTreeMap<String, Object>>) responseMap
				.get("components");
		List<SonarProjectComponent> projectsComponents = new ArrayList<>(components.size());
		components.forEach(componentMap -> {
			SonarProjectComponent spc = new SonarProjectComponent();
			spc.setId((String) componentMap.get("id"));
			spc.setOrganization((String) componentMap.get("organization"));
			spc.setKey((String) componentMap.get("key"));
			spc.setName((String) componentMap.get("name"));
			spc.setTags((List<String>) componentMap.get("tags"));
			projectsComponents.add(spc);
		});
		return projectsComponents;
	}

	private List<Project> requestsProjectsForMoreThanOnePage(Long total)
			throws IOException, InterruptedException {
		int listSize = total.intValue() - DEFAULT_PAGE_SIZE;
		List<Project> projects = new ArrayList<>(listSize);

		int retrievedSoFar = DEFAULT_PAGE_SIZE;
		int currentPage = 2;

		while (retrievedSoFar < total && retrievedSoFarWithinLimitsOfSonarCloud(retrievedSoFar)) {
			String urlForPage = buildUrlForPage(currentPage);
			projects.addAll(requestProjects(urlForPage));
			System.out.println("\nretrieving page " + currentPage + "...");

			currentPage++;
			retrievedSoFar += DEFAULT_PAGE_SIZE;
		}
		return projects;
	}

	private boolean retrievedSoFarWithinLimitsOfSonarCloud(int retrievedSoFar) {
		if (isSonarCloud) {
			boolean withinLimits = retrievedSoFar < MAX_RESULTS_THAT_SONAR_RETURN;
			if (!withinLimits) {
				System.out.println("Reached maximum SonarCloud results ("
						+ MAX_RESULTS_THAT_SONAR_RETURN + "). Stopping further requests.");
			}
			return withinLimits;
		}
		// not sure if private SonarQube have restrictions of return size.
		return true;
	}

	private String buildUrlForPage(Integer page) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + PROJECTS_SEARCH_URL).newBuilder();
		urlBuilder.addQueryParameter("filter", "languages=java");
		urlBuilder.addQueryParameter("ps", DEFAULT_PAGE_SIZE.toString());
		urlBuilder.addQueryParameter("p", page.toString());
		return urlBuilder.build().toString();
	}

	private List<Project> requestProjects(String url) throws IOException, InterruptedException {
		String responseBody = retrieveResponseBodyForURL(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		List<SonarProjectComponent> projectsComponents = retrieveSonarProjectComponentsFromResponseMap(
				responseMap);
		return projectsComponents.stream().map(SonarProjectComponent::toProject)
				.collect(Collectors.toList());
	}

}
