package br.unb.cloudissues.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import br.unb.cloudissues.model.Issue;
import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.Resolutions;
import br.unb.cloudissues.model.Statuses;
import br.unb.cloudissues.model.Violations;
import br.unb.cloudissues.util.Utils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ViolationsRetriever {

	private static final String DEFAULT_SONAR_CLOUD_URL = "https://sonarcloud.io/api/issues/search";

	// SonarCloud is not returning any value after the 10_000th
	private static final Integer MAX_RESULTS_THAT_SONAR_RETURN = 10_000;

	private static final Integer DEFAULT_PAGE_SIZE = 500;

	private static final int DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS = 6500;
	private static final int DEFAULT_MAX_REQUESTS_TO_WAIT = 10;

	private static int count = 1;

	private final boolean isSonarCloud;

	private final String baseUrl;

	private final int timeToWaitInMiliSecondsBetweenRequests;
	private final int maxRequestsToWait;

	private final List<Resolutions> resolutions;

	private final List<Statuses> statuses;

	private final Integer pageSize;

	private final Integer maxResultsForProject;

	private final boolean olderVersion;

	private final OkHttpClient httpClient = new OkHttpClient().newBuilder()
			.connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS).build();

	private ViolationsRetriever(Builder builder) {
		isSonarCloud = Optional.ofNullable(builder.isSonarCloud).orElse(true);
		baseUrl = Optional.ofNullable(builder.baseUrl).orElse(DEFAULT_SONAR_CLOUD_URL);
		resolutions = Optional.ofNullable(builder.resolutions).orElse(Collections.emptyList());
		statuses = Optional.ofNullable(builder.statuses).orElse(Collections.emptyList());
		timeToWaitInMiliSecondsBetweenRequests = Optional
				.ofNullable(builder.timeToWaitInMiliSecondsBetweenRequests)
				.orElse(DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS);
		maxRequestsToWait = Optional.ofNullable(builder.maxRequestsToWait)
				.orElse(DEFAULT_MAX_REQUESTS_TO_WAIT);
		pageSize = Optional.ofNullable(builder.pageSize).orElse(DEFAULT_PAGE_SIZE);
		maxResultsForProject = Optional.ofNullable(builder.maxResultsForProject)
				.orElse(Integer.MAX_VALUE);
		olderVersion = builder.olderVersion;
	}

	public static class Builder {

		private Boolean isSonarCloud;
		private String baseUrl;
		private List<Resolutions> resolutions;
		private List<Statuses> statuses;
		private Integer timeToWaitInMiliSecondsBetweenRequests;
		private Integer maxRequestsToWait;
		private Integer pageSize;
		private Integer maxResultsForProject;
		private boolean olderVersion = false;

		public Builder withIsSonarCloud(boolean isSonarCloud) {
			this.isSonarCloud = isSonarCloud;
			return this;
		}

		// TODO change how the baseUrl works, just like JavaProjectsRetriever
		public Builder withBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder withResolutions(Resolutions... resolutions) {
			this.resolutions = Arrays.asList(resolutions);
			return this;
		}

		public Builder withStatuses(Statuses... statuses) {
			this.statuses = Arrays.asList(statuses);
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

		public Builder withPageSize(Integer pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		public Builder withMaxResultsForProject(Integer maxResultsForProject) {
			this.maxResultsForProject = maxResultsForProject;
			return this;
		}

		public Builder olderVersion(boolean olderVersion) {
			this.olderVersion = olderVersion;
			return this;
		}

		public ViolationsRetriever build() {
			return new ViolationsRetriever(this);
		}

	}

	public List<Violations> retrieve(List<Project> projects) {
		List<Violations> violations = new ArrayList<>();

		count = 1;

		projects.stream().forEach(project -> {
			try {
				System.out.println("\nretrieving " + project.getProjectKey() + " ...");
				violations.add(httpRequestViolationsForProject(project));
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		return violations;
	}

	private String buildURL(String projectKey) {
		return doBuildURL(baseUrl, projectKey).build().toString();
	}

	private HttpUrl.Builder doBuildURL(String url, String projectKey) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
		urlBuilder.addQueryParameter(getPageSizeQueryParamKey(), pageSize.toString());

		urlBuilder.addQueryParameter(getComponentKeysQueryParamKey(), projectKey);

		if (resolutions != null && !resolutions.isEmpty()) {
			urlBuilder.addQueryParameter("resolutions",
					String.join(",", getResolutionsValuesAsArray()));
		}

		if (statuses != null && !statuses.isEmpty()) {
			urlBuilder.addQueryParameter("statuses", String.join(",", getStatusesValuesAsArray()));
		}

		return urlBuilder;
	}

	private String getPageSizeQueryParamKey() {
		if (!olderVersion)
			return "ps";
		return "pageSize";
	}

	private String getComponentKeysQueryParamKey() {
		if (!olderVersion)
			return "componentKeys";
		return "componentRoots";
	}

	private String[] getResolutionsValuesAsArray() {
		List<String> resolutionsValues = resolutions.stream().map(Resolutions::getValue)
				.collect(Collectors.toList());
		return resolutionsValues.toArray(new String[0]);
	}

	private String[] getStatusesValuesAsArray() {
		List<String> statusesValues = statuses.stream().map(Statuses::toString)
				.collect(Collectors.toList());
		return statusesValues.toArray(new String[0]);
	}

	private Violations httpRequestViolationsForProject(Project project)
			throws IOException, InterruptedException {
		String projectKey = project.getProjectKey();

		String url = buildURL(projectKey);

		String responseBody = retrieveResponseBodyForURL(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		Long total = retrieveTotal(responseMap);

		List<Issue> issues = new ArrayList<>(total.intValue());
		@SuppressWarnings("unchecked")
		List<Issue> firstPageIssues = Optional.ofNullable((List<Issue>) responseMap.get("issues"))
				.orElse(Collections.emptyList());
		issues.addAll(firstPageIssues);

		if (total > pageSize) {
			System.out.println("total: " + total);
			System.out.println("requesting for more pages.");
			issues.addAll(requestsIssuesForMoreThanOnePage(total, projectKey));
		}

		return new Violations(project, total, issues);
	}

	@SuppressWarnings("unchecked")
	private Long retrieveTotal(Map<String, Object> responseMap) {
		final String totalKey = "total";
		if (responseMap.containsKey(totalKey)) {
			return (((Double) responseMap.get(totalKey))).longValue();
		} else {
			Map<String, Double> paging = (Map<String, Double>) responseMap.get("paging");
			return paging.get(totalKey).longValue();
		}
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

	private List<Issue> requestsIssuesForMoreThanOnePage(Long total, String projectKey)
			throws IOException, InterruptedException {
		int listSize = total.intValue() - pageSize;
		List<Issue> issues = new ArrayList<>(listSize);

		int retrievedSoFar = pageSize;
		int currentPage = 2;

		while (shouldRequestIssuesForMorePages(retrievedSoFar, total)) {
			String urlForPage = buildUrlForPage(projectKey, currentPage);
			issues.addAll(requestIssues(urlForPage));
			System.out.print("\nretrieving page " + currentPage + "...");

			currentPage++;
			retrievedSoFar += pageSize;
		}
		System.out.println();
		return issues;
	}

	private boolean shouldRequestIssuesForMorePages(int retrievedSoFar, Long total) {
		if (retrievedSoFar >= maxResultsForProject) {
			System.out.println(
					"Not requesting more results because already retrieved max results for project ("
							+ maxResultsForProject + ").");
			return false;
		}
		return retrievedSoFar < total && retrievedSoFarWithinLimitsOfSonarCloud(retrievedSoFar);
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

	private String buildUrlForPage(String projectKey, Integer page) {
		HttpUrl.Builder urlBuilder = doBuildURL(baseUrl, projectKey);
		urlBuilder.addQueryParameter("p", page.toString());
		return urlBuilder.build().toString();
	}

	private List<Issue> requestIssues(String url) throws IOException, InterruptedException {
		String responseBody = retrieveResponseBodyForURL(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		@SuppressWarnings("unchecked")
		List<Issue> issues = (List<Issue>) responseMap.get("issues");
		return Optional.ofNullable(issues).orElse(Collections.emptyList());
	}

}
