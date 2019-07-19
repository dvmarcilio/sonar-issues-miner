package br.unb.cloudissues;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.unb.cloudissues.http.JavaProjectsCSVRetriever;
import br.unb.cloudissues.http.JavaProjectsRetriever;
import br.unb.cloudissues.http.JavaRulesRetriever;
import br.unb.cloudissues.http.ProjectFilesRetriever;
import br.unb.cloudissues.http.ViolationsRetriever;
import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.model.ProjectFiles;
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
	private static final String SONAR_API_URL = System.getenv("SONAR_API_URL");

	private static final String ISSUES_SEARCH_URL = SONAR_API_URL + "/issues/search";

	private static final String DIRECTORY = "resources/";
	private static final String FIXED_DIRECTORY = DIRECTORY + "fixed/";
	private static final String OPEN_DIRECTORY = DIRECTORY + "open-issues/";

	private static final String RULES_LIST = DIRECTORY + "java_rules.json";
	private static final String PROJECTS_LIST = DIRECTORY + "java_project_list.json";

	private static final String WONT_FIX_FALSE_POSITIVE_DIRECTORY = DIRECTORY
			+ "wont-fix-false-positive/";

	private static final String FILES_METRICS_DIRECTORY = DIRECTORY + "files-metrics/";

	static {
		if (SONAR_API_URL == null || SONAR_API_URL.isEmpty())
			throw new IllegalStateException("Please change the value of SONAR_API_URL");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		retrieveAndWriteRules();

		retrieveAndWriteProjects();
//		retrieveAndWriteProjectsIfApache();

		requestAndWriteFixedViolationsOneFilePerProject(PROJECTS_LIST, FIXED_DIRECTORY);

		requestAndWriteOpenViolationsOneFilePerProject(PROJECTS_LIST, OPEN_DIRECTORY);

		requestAndWriteFalsePositiveAndWontFixViolations(PROJECTS_LIST,
				WONT_FIX_FALSE_POSITIVE_DIRECTORY);

		projectsAndFilesMetrics();
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

	static void retrieveAndWriteProjectsIfApache() throws IOException {
		JavaProjectsCSVRetriever jpr = new JavaProjectsCSVRetriever(SONAR_API_URL + "/projects");
		Utils.writeObjToFileAsJSON(jpr.retrieve(), PROJECTS_LIST);
	}

	private static void requestAndWriteFixedViolationsOneFilePerProject(String projectsJsonPath,
			String fixedDirectory) throws IOException, InterruptedException {
		System.out.println("\nRetrieving fixed violations");

		List<Project> projects = Utils.retrieveCollectionFromJSONFile(projectsJsonPath,
				Project.class);

		for (Project project : projects) {
			requestAndWriteFixedViolationsForProjects(Utils.generateJsonPathToSaveForEachProject(
					fixedDirectory, project.getProjectName()), Collections.singletonList(project));
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
		System.out.println("\nRetrieving open violations");

		List<Project> projects = Utils.retrieveCollectionFromJSONFile(projectsJsonPath,
				Project.class);

		for (Project project : projects) {
			requestAndWriteOpenViolationsForProjects(Utils.generateJsonPathToSaveForEachProject(
					openDirectory, project.getProjectName()), Collections.singletonList(project));
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

	private static void requestAndWriteFalsePositiveAndWontFixViolations(String projectsJsonPath,
			String wontFixFalsePositiveDirectory) throws IOException, InterruptedException {
		System.out.println("\nRetrieving false positive and wont fix violations");

		List<Project> projects = Utils.retrieveCollectionFromJSONFile(projectsJsonPath,
				Project.class);

		for (Project project : projects) {
			requestAndWriteFalsePositiveViolationsForProjects(
					Utils.generateJsonPathToSaveForEachProject(wontFixFalsePositiveDirectory,
							project.getProjectName()), Collections.singletonList(project));
			waitBetweenPartitions(10_000);
		}
	}

	private static void requestAndWriteFalsePositiveViolationsForProjects(String jsonPathToSave,
			List<Project> projects) throws IOException {
		ViolationsRetriever violationsRetriever = new ViolationsRetriever.Builder()
				.withIsSonarCloud(false) //
				.withBaseUrl(ISSUES_SEARCH_URL) //
				.withMaxRequestsToWait(15) //
				.withTimeToWaitInMiliSecondsBetweenRequests(10_000)
				.withResolutions(Resolutions.FALSE_POSITIVE, Resolutions.WONTFIX) //
				.build();

		List<Violations> viols = violationsRetriever.retrieve(projects);

		Utils.writeObjToFileAsJSON(viols, jsonPathToSave);
	}

	private static void projectsAndFilesMetrics() throws IOException {
		System.out.println("\nRetrieving files and metrics");

		ProjectFilesRetriever pfr = new ProjectFilesRetriever.Builder(SONAR_API_URL)
				.maxRequestsToWait(25).timeToWaitInMiliSecondsBetWeenRequests(10_000).build();
		List<Project> projects = Utils.retrieveCollectionFromJSONFile(PROJECTS_LIST, Project.class);
		List<ProjectFiles> projectFilesList = pfr.retrieve(projects);

		projectFilesList.forEach(projectFiles -> {
			try {
				Utils.writeObjToFileAsJSON(projectFiles, FILES_METRICS_DIRECTORY + Utils
						.sanitizeProjectName(projectFiles.getProject().getProjectName() + ".json"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}
