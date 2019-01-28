package br.unb.cloudissues.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.ProjectFile;
import br.unb.cloudissues.model.ProjectFiles;
import br.unb.cloudissues.util.UnsafeOkHttpClient;
import br.unb.cloudissues.util.Utils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProjectFilesRetriever {

	private static final String PROJECT_URL_SUFFIX = "/measures/component_tree";

	private static final String ECLIPSE_URL_SUFFIX = "/resources";

	private static int count = 1;

	private static final Integer DEFAULT_PAGE_SIZE = 500;

	private static final Integer MAX_RESULTS_THAT_SONAR_RETURN = 10_000;

	private static final int DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS = 6500;
	private static final int DEFAULT_MAX_REQUESTS_TO_WAIT = 10;

	private final String baseUrl;
	private final boolean hasMaxResultsLimit;
	private final int timeToWaitInMiliSecondsBetweenRequests;
	private final int maxRequestsToWait;
	private final boolean ignoreSSL;
	private final boolean isApache;
	private final boolean isEclipse;

	private final OkHttpClient httpClient;

	private ProjectFilesRetriever(Builder builder) {
		baseUrl = builder.baseUrl;
		hasMaxResultsLimit = Optional.ofNullable(builder.hasMaxResultsLimit).orElse(false);
		timeToWaitInMiliSecondsBetweenRequests = Optional
				.ofNullable(builder.timeToWaitInMiliSecondsBetweenRequests)
				.orElse(DEFAULT_TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS);
		maxRequestsToWait = Optional.ofNullable(builder.maxRequestsToWait)
				.orElse(DEFAULT_MAX_REQUESTS_TO_WAIT);
		ignoreSSL = Optional.ofNullable(builder.ignoreSSL).orElse(false);
		isApache = Optional.ofNullable(builder.isApache).orElse(false);
		isEclipse = Optional.ofNullable(builder.isEclipse).orElse(false);
		httpClient = createHttpClient();
	}

	private OkHttpClient createHttpClient() {
		if (ignoreSSL) {
			return UnsafeOkHttpClient.getUnsafeOkHttpClient();
		}
		return new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();
	}

	public static class Builder {

		private final String baseUrl;
		private Boolean hasMaxResultsLimit;
		private Integer timeToWaitInMiliSecondsBetweenRequests;
		private Integer maxRequestsToWait;
		private Boolean ignoreSSL;
		private Boolean isApache;
		private Boolean isEclipse;

		public Builder(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public Builder hasMaxResultsLimit(boolean hasMaxResultsLimit) {
			this.hasMaxResultsLimit = hasMaxResultsLimit;
			return this;
		}

		public Builder timeToWaitInMiliSecondsBetWeenRequests(
				int timeToWaitInMiliSecondsBetweenRequests) {
			this.timeToWaitInMiliSecondsBetweenRequests = timeToWaitInMiliSecondsBetweenRequests;
			return this;
		}

		public Builder maxRequestsToWait(int maxRequestsToWait) {
			this.maxRequestsToWait = maxRequestsToWait;
			return this;
		}

		public Builder ignoreSSL(boolean ignoreSSL) {
			this.ignoreSSL = ignoreSSL;
			return this;
		}

		public Builder isApache() {
			this.isApache = true;
			return this;
		}

		public ProjectFilesRetriever build() {
			return new ProjectFilesRetriever(this);
		}

		public Builder isEclipse() {
			this.isEclipse = true;
			return this;
		}

	}

	public List<ProjectFiles> retrieve(List<Project> projects) {
		List<ProjectFiles> projectsFiles = new ArrayList<>(projects.size());

		count = 1;

		projects.stream().forEach(project -> {
			try {
				System.out.println(
						"\nretrieving files and metrics for " + project.getProjectKey() + "...");
				projectsFiles.add(httpRequestForFilesAndMetricsForProject(project));
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		return projectsFiles;
	}

	private ProjectFiles httpRequestForFilesAndMetricsForProject(Project project)
			throws IOException, InterruptedException {
		String projectKey = project.getProjectKey();
		ProjectFiles projectFiles = new ProjectFiles(project, getMetricsForProject(projectKey));
		projectFiles.setFiles(httpRequestForFilesInProject(projectKey));
		return projectFiles;
	}

	private Map<String, String> getMetricsForProject(String projectKey)
			throws IOException, InterruptedException {
		if (!isEclipse) {
			return getMetricsForProjectsNonEclipse(projectKey);
		}
		return Collections.emptyMap();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, String> getMetricsForProjectsNonEclipse(String projectKey)
			throws IOException, InterruptedException {
		String url = buildURLForProject(projectKey);
		String responseBody = retrieveResponseBodyForUrl(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);

		ArrayList<LinkedHashMap<String, Object>> measures = (ArrayList<LinkedHashMap<String, Object>>) ((Map) responseMap
				.get("baseComponent")).get("measures");

		return metricsMapFromMeasuresMap(measures);
	}

	private Map<String, String> metricsMapFromMeasuresMap(
			ArrayList<LinkedHashMap<String, Object>> measures) {
		Map<String, String> metrics = new HashMap<String, String>();
		for (Map<String, Object> measureMap : measures) {
			metrics.put(measureMap.get("metric").toString(), measureMap.get("value").toString());
		}

		return metrics;
	}

	private String buildURLForProject(String projectKey) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + PROJECT_URL_SUFFIX).newBuilder();
		urlBuilder.addQueryParameter(componentQueryParamKey(), projectKey);
		urlBuilder.addQueryParameter("qualifiers", "TRK");
		urlBuilder.addQueryParameter("metricKeys",
				"complexity_in_classes,last_commit_date,ncloc,sqale_rating,overall_coverage,"
						+ "alert_status,reliability_rating,security_rating,classes");
		return urlBuilder.build().toString();
	}

	private String retrieveResponseBodyForUrl(String url) throws IOException, InterruptedException {
		tryNotToFloodSonarWithLotsOfRequests();
		return doRetrieveResponseBodyForUrl(url);
	}

	private String doRetrieveResponseBodyForUrl(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();
		Response response = httpClient.newCall(request).execute();
		return response.body().string();
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

	private Set<ProjectFile> httpRequestForFilesInProject(String projectKey)
			throws IOException, InterruptedException {
		if (!isEclipse) {
			return httpRequestForFilesNotEclipse(projectKey);
		}
		return httpRequestForFilesEclipse(projectKey);
	}

	private Set<ProjectFile> httpRequestForFilesNotEclipse(String projectKey)
			throws IOException, InterruptedException {
		String url = buildUrlForFiles(projectKey);
		String responseBody = retrieveResponseBodyForUrl(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);

		ArrayList<Map<String, Object>> components = retrieveComponentsListFromResponseMap(
				responseMap);
		Long total = retrieveTotalFromResponseMap(responseMap);

		Set<ProjectFile> projectFiles = new HashSet<>(total.intValue());
		projectFiles.addAll(parseComponentsListToSetOfProjectFiles(components));

		if (total > DEFAULT_PAGE_SIZE) {
			System.out.println("total: " + total);
			System.out.println("requesting for more pages.");
			projectFiles.addAll(requestFilesForMoreThanOnePage(total, projectKey));
		}

		return projectFiles;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Map<String, Object>> retrieveComponentsListFromResponseMap(
			Map<String, Object> responseMap) {
		return (ArrayList<Map<String, Object>>) responseMap.get("components");
	}

	@SuppressWarnings("rawtypes")
	private long retrieveTotalFromResponseMap(Map<String, Object> responseMap) {
		return ((Double) ((Map) responseMap.get("paging")).get("total")).longValue();
	}

	private String buildUrlForFiles(String projectKey) {
		HttpUrl.Builder urlBuilder = doBuildURLForFiles(projectKey);
		return urlBuilder.build().toString();
	}

	private HttpUrl.Builder doBuildURLForFiles(String projectKey) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + PROJECT_URL_SUFFIX).newBuilder();
		urlBuilder.addQueryParameter(componentQueryParamKey(), projectKey);
		urlBuilder.addQueryParameter("qualifiers", "FIL");
		urlBuilder.addQueryParameter("q", ".java");
		urlBuilder.addQueryParameter("metricKeys", metricsKeysQueryParamValue());
		urlBuilder.addQueryParameter("ps", DEFAULT_PAGE_SIZE.toString());
		return urlBuilder;
	}

	private String metricsKeysQueryParamValue() {
		String metricsCommonForNewerAndApache = "ncloc,overall_coverage,coverage,lines,complexity";
		if (isApache) {
			return metricsCommonForNewerAndApache;
		}
		return metricsCommonForNewerAndApache + ",cognitive_complexity";
	}

	private String componentQueryParamKey() {
		if (isApache)
			return "baseComponentKey";
		return "component";
	}

	@SuppressWarnings("unchecked")
	private Set<ProjectFile> parseComponentsListToSetOfProjectFiles(
			ArrayList<Map<String, Object>> components) {
		Set<ProjectFile> projectFiles = new HashSet<>(components.size());
		for (Map<String, Object> component : components) {
			projectFiles
					.add(new ProjectFile(component.get("key").toString(), metricsMapFromMeasuresMap(
							(ArrayList<LinkedHashMap<String, Object>>) component.get("measures"))));
		}
		return projectFiles;
	}

	private Set<ProjectFile> requestFilesForMoreThanOnePage(Long total, String projectKey)
			throws IOException, InterruptedException {
		int setSize = total.intValue() - DEFAULT_PAGE_SIZE;
		Set<ProjectFile> projectFiles = new HashSet<>(setSize);

		int retrievedSoFar = DEFAULT_PAGE_SIZE;
		int currentPage = 2;

		while (shouldRequestIssuesForMorePages(retrievedSoFar, total)) {
			String urlForPage = buildUrlForPage(projectKey, currentPage);
			projectFiles.addAll(requestProjectFiles(urlForPage));
			System.out.print("\nretrieving page " + currentPage + "...");

			currentPage++;
			retrievedSoFar += DEFAULT_PAGE_SIZE;
		}

		return projectFiles;
	}

	private String buildUrlForPage(String projectKey, Integer page) {
		HttpUrl.Builder urlBuilder = doBuildURLForFiles(projectKey);
		urlBuilder.addQueryParameter("p", page.toString());
		return urlBuilder.build().toString();
	}

	private Set<ProjectFile> requestProjectFiles(String url)
			throws IOException, InterruptedException {
		String responseBody = retrieveResponseBodyForUrl(url);
		Map<String, Object> responseMap = Utils.responseToMap(responseBody);
		ArrayList<Map<String, Object>> components = retrieveComponentsListFromResponseMap(
				responseMap);
		return parseComponentsListToSetOfProjectFiles(components);
	}

	private boolean shouldRequestIssuesForMorePages(int retrievedSoFar, Long total) {
		if (retrievedSoFar >= MAX_RESULTS_THAT_SONAR_RETURN) {
			System.out.println(
					"Not requesting more results because already retrieved max results for project ("
							+ MAX_RESULTS_THAT_SONAR_RETURN + ").");
			return false;
		}
		return retrievedSoFar < total && retrievedSoFarWithinLimitsOfSonarCloud(retrievedSoFar);
	}

	private boolean retrievedSoFarWithinLimitsOfSonarCloud(int retrievedSoFar) {
		if (hasMaxResultsLimit) {
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

	private Set<ProjectFile> httpRequestForFilesEclipse(String projectKey)
			throws IOException, InterruptedException {
		String url = doBuildURLForFilesEclipse(projectKey);
		String responseBody = retrieveResponseBodyForUrl(url);

		List<Map<String, Object>> responseMaps = Utils.responseToListOfMap(responseBody);

		return responseMaps.stream()
				.filter(responseMap -> responseMap.get("key").toString().endsWith(".java"))
				.map(responseMap -> new ProjectFile(responseMap.get("key").toString(),
						Collections.emptyMap()))
				.collect(Collectors.toSet());
	}

	private String doBuildURLForFilesEclipse(String projectKey) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + ECLIPSE_URL_SUFFIX).newBuilder();
		urlBuilder.addQueryParameter("resource", projectKey);
		urlBuilder.addQueryParameter("depth", "-1");
		urlBuilder.addQueryParameter("scopes", "FIL");
		urlBuilder.addQueryParameter("format", "json");
		return urlBuilder.build().toString();
	}

}
