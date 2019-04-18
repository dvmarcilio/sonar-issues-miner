package br.unb.cloudissues.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import br.unb.cloudissues.model.Rule;
import br.unb.cloudissues.util.Utils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JavaRulesRetriever {

	private static final Integer DEFAULT_PAGE_SIZE = 500;
	private static final int TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS = 6500;
	private static final int MAX_REQUESTS_TO_WAIT = 10;

	private static final String RULES_SEARCH_URL = "/rules/search";

	private static int count = 1;

	private final OkHttpClient httpClient = new OkHttpClient();

	private final String baseUrl;

	public JavaRulesRetriever(String baseUrl) {
		Objects.requireNonNull(baseUrl);
		this.baseUrl = baseUrl;
	}

	public List<Rule> retrieve() throws IOException, InterruptedException {
		count = 1;
		System.out.println("\nRetrieving Java rules in " + baseUrl + RULES_SEARCH_URL);
		return httpRequestJavaRules();
	}

	@SuppressWarnings("unchecked")
	private List<Rule> httpRequestJavaRules() throws IOException, InterruptedException {
		String rulesResponse = requestRulesResponseAsString(buildURLForJavaRules());

		Map<String, Object> responseMap = Utils.responseToMap(rulesResponse);

		Long total = ((Double) responseMap.get("total")).longValue();

		List<Rule> allRules = new ArrayList<>(total.intValue());

		List<Rule> currentRules = (List<Rule>) responseMap.get("rules");
		allRules.addAll(currentRules);

		if (total > DEFAULT_PAGE_SIZE) {
			allRules.addAll(requestRulesForMoreThanOnePage(total));
		}

		return allRules;
	}

	private String requestRulesResponseAsString(String url)
			throws IOException, InterruptedException {
		tryNotToFloodSonarWithLotsOfRequests();
		return doRequestRulesResponseAsString(url);
	}

	private void tryNotToFloodSonarWithLotsOfRequests() throws InterruptedException {
		if (count % MAX_REQUESTS_TO_WAIT == 0) {
			System.out.println("\nWaiting for "
					+ (TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS / 1000.0 + " seconds..."));
			System.out.println(count + " requests so far.");
			Thread.sleep(TIME_TO_WAIT_IN_MILISECONDS_BETWEEN_REQUESTS);
		}
		count++;
	}

	private String doRequestRulesResponseAsString(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();
		Response response = httpClient.newCall(request).execute();
		return response.body().string();
	}

	private String buildURLForJavaRules() {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + RULES_SEARCH_URL).newBuilder();
		urlBuilder.addQueryParameter("languages", "java");
		urlBuilder.addQueryParameter("ps", "500");
		return urlBuilder.build().toString();
	}

	private List<Rule> requestRulesForMoreThanOnePage(Long total)
			throws IOException, InterruptedException {
		int listSize = total.intValue() - DEFAULT_PAGE_SIZE;
		List<Rule> remainingRules = new ArrayList<>(listSize);

		int retrievedSoFar = DEFAULT_PAGE_SIZE;
		int currentPage = 2;

		while (retrievedSoFar < total) {
			String urlForPage = buildUrlForPage(currentPage);
			remainingRules.addAll(requestRules(urlForPage));

			System.out.println("\nretrieving page " + currentPage + "...");

			currentPage++;
			retrievedSoFar += DEFAULT_PAGE_SIZE;
		}

		return remainingRules;
	}

	private String buildUrlForPage(Integer page) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + RULES_SEARCH_URL).newBuilder();
		urlBuilder.addQueryParameter("languages", "java");
		urlBuilder.addQueryParameter("ps", "500");
		urlBuilder.addQueryParameter("p", page.toString());
		return urlBuilder.build().toString();
	}

	@SuppressWarnings("unchecked")
	private List<Rule> requestRules(String url) throws IOException, InterruptedException {
		String rulesResponse = requestRulesResponseAsString(url);
		Map<String, Object> responseMap = Utils.responseToMap(rulesResponse);
		return (List<Rule>) responseMap.get("rules");
	}

}
