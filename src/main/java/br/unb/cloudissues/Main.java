package br.unb.cloudissues;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import br.unb.cloudissues.http.JavaProjectsRetriever;
import br.unb.cloudissues.http.JavaRulesRetriever;
import br.unb.cloudissues.http.ViolationsRetriever;
import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.Resolutions;
import br.unb.cloudissues.model.Rule;
import br.unb.cloudissues.model.Statuses;
import br.unb.cloudissues.model.Violations;
import br.unb.cloudissues.util.Utils;

public class Main {

	// XXX CHANGE THIS LINE. should be your sonar URL until '/api'
	// example: "https://sonar.eclipse.org/api/"
	// example: "https://builds.apache.org/analysis/api"
	// example: "https://sonarcloud.io/api"
	private static final String SONAR_API_URL = "";

	private static final String ISSUES_SEARCH_URL = SONAR_API_URL + "/issues/search";

	private static final String DIRECTORY = "resources/";
	private static final String FIXED_DIRECTORY = DIRECTORY + "fixed/";
	private static final String OPEN_DIRECTORY = DIRECTORY + "open-issues/";

	private static final String RULES_LIST = DIRECTORY + "java_rules.json";
	private static final String PROJECTS_LIST = DIRECTORY + "java_project_list.json";
	
	static {
		if (SONAR_API_URL == null || SONAR_API_URL.isEmpty())
			throw new IllegalStateException("Please change the value of SONAR_API_URL");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		retrieveAndWriteRules();

		retrieveAndWriteProjects();

		requestAndWriteFixedViolationsOneFilePerProject(PROJECTS_LIST, FIXED_DIRECTORY);

		requestAndWriteOpenViolationsOneFilePerProject(PROJECTS_LIST, OPEN_DIRECTORY);
	}

	private static void retrieveAndWriteRules() throws IOException, InterruptedException {
		JavaRulesRetriever jrr = new JavaRulesRetriever(SONAR_API_URL);
		List<Rule> rules = jrr.retrieve();
		Utils.writeObjToFileAsJSON(rules, RULES_LIST);
	}

	private static void retrieveAndWriteProjects() throws IOException, InterruptedException {
		JavaProjectsRetriever jpr = new JavaProjectsRetriever.Builder(SONAR_API_URL)
				.isSonarCloud(false).build();
		List<Project> sonarJavaProjects = jpr.retrieve();
		Utils.writeObjToFileAsJSON(sonarJavaProjects, PROJECTS_LIST);
	}

	private static void requestAndWriteFixedViolationsOneFilePerProject(String projectsJsonPath,
			String fixedDirectory) throws IOException, InterruptedException {
		List<Project> projects = Utils.retrieveCollectionFromJSONFile(projectsJsonPath,
				Project.class);

		for (Project project : projects) {
			requestAndWriteFixedViolationsForProjects(Utils.generateJsonPathToSaveForEachProject(
					fixedDirectory, project.getProjectName()), Arrays.asList(project));
			waitBetweenPartitions(10_000);
		}
	}

	private static void requestAndWriteFixedViolationsForProjects(String jsonPathToSave,
			List<Project> projects) throws IOException {
		ViolationsRetriever violationsRetriever = new ViolationsRetriever.Builder()
				.withIsSonarCloud(false) //
				.withBaseUrl(ISSUES_SEARCH_URL) //
				.withMaxRequestsToWait(15) //
				.withTimeToWaitInMiliSecondsBetweenRequests(10_000)
				.withResolutions(Resolutions.FIXED) //
				.build();

		List<Violations> viols = violationsRetriever.retrieve(projects);

		Utils.writeObjToFileAsJSON(viols, jsonPathToSave);
	}

	private static void waitBetweenPartitions(int timeInMiliSeconds) throws InterruptedException {
		System.out.println(
				"\nwaiting " + (timeInMiliSeconds / 1000) + " seconds between partitions...");
		Thread.sleep(timeInMiliSeconds);
	}

	private static void requestAndWriteOpenViolationsOneFilePerProject(String projectsJsonPath,
			String openDirectory) throws IOException, InterruptedException {
		List<Project> projects = Utils.retrieveCollectionFromJSONFile(projectsJsonPath,
				Project.class);

		for (Project project : projects) {
			requestAndWriteOpenViolationsForProjects(Utils.generateJsonPathToSaveForEachProject(
					openDirectory, project.getProjectName()), Arrays.asList(project));
			waitBetweenPartitions(10_000);
		}
	}

	private static void requestAndWriteOpenViolationsForProjects(String jsonPathToSave,
			List<Project> projects) throws IOException {
		ViolationsRetriever violationsRetriever = new ViolationsRetriever.Builder()
				.withIsSonarCloud(false) //
				.withBaseUrl(ISSUES_SEARCH_URL) //
				.withMaxRequestsToWait(15) //
				.withTimeToWaitInMiliSecondsBetweenRequests(10_000) //
				.withStatuses(Statuses.OPEN) //
				.build();

		List<Violations> viols = violationsRetriever.retrieve(projects);

		Utils.writeObjToFileAsJSON(viols, jsonPathToSave);
	}

}
