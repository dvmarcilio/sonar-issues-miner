package br.unb.cloudissues.http;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import br.unb.cloudissues.model.Project;
import br.unb.cloudissues.util.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// https://builds.apache.org/analysis/web_api/api/projects
public class JavaProjectsCSVRetriever {

	private final OkHttpClient httpClient = new OkHttpClient();

	private final XmlMapper xmlMapper;

	private Path projectsCSVPath;

	private String url;

	public JavaProjectsCSVRetriever(Path projectsCSVPath) {
		xmlMapper = initXmlMapper();
		Objects.requireNonNull(projectsCSVPath);
		this.projectsCSVPath = projectsCSVPath;
	}

	private XmlMapper initXmlMapper() {
		JacksonXmlModule module = new JacksonXmlModule();
		module.setDefaultUseWrapper(false);
		return new XmlMapper(module);
	}

	public JavaProjectsCSVRetriever(String url) {
		xmlMapper = initXmlMapper();
		Objects.requireNonNull(url);
		this.url = url;
	}

	public List<Project> retrieve() throws IOException {
		if (url != null) {
			return retrieveProjectsFromURL();
		}
		return retrieveProjectsFromCSVFilePath();
	}

	private List<Project> retrieveProjectsFromURL() throws IOException {
		return mapProjectsAsXmlToProjectsList(retrieveProjectsAsMapsFromXMLResponse());
	}

	private List<LinkedHashMap<String, String>> retrieveProjectsAsMapsFromXMLResponse()
			throws IOException {
		String xmlAsString = retrieveResponseBodyForURL(url);
		Projects projects = xmlMapper.readValue(xmlAsString, Projects.class);
		return projects.projectsListOfMaps;
	}

	private String retrieveResponseBodyForURL(String url) throws IOException {
		System.out.println("\nRetrieving projects in " + url);
		return doRetrieveResponseBodyForURL(url);
	}

	private String doRetrieveResponseBodyForURL(String url) throws IOException {
		Request request = new Request.Builder().url(url)
				.addHeader("Content-type", "application/xml").build();
		Response response = httpClient.newCall(request).execute();
		return response.body().string();
	}

	private List<Project> mapProjectsAsXmlToProjectsList(
			List<LinkedHashMap<String, String>> projectsXml) {
		return projectsXml.stream().map(projectXmlMap -> {
			Project p = new Project();
			p.setId(getValueFromMapOrThrow(projectXmlMap, "id"));
			p.setProjectKey(getValueFromMapOrThrow(projectXmlMap, "key"));
			p.setProjectName(getValueFromMapOrThrow(projectXmlMap, "name"));
			return p;
		}).collect(Collectors.toList());
	}

	private String getValueFromMapOrThrow(Map<String, String> map, String key) {
		Optional<String> value = Optional.ofNullable(map.get(key));
		return value.orElseThrow(IllegalStateException::new);
	}

	private List<Project> retrieveProjectsFromCSVFilePath() throws IOException {
		String xmlAsString = Utils.readAllLines(projectsCSVPath.toString());
		Projects projects = xmlMapper.readValue(xmlAsString, Projects.class);
		return mapProjectsAsXmlToProjectsList(projects.projectsListOfMaps);
	}

	@JacksonXmlRootElement(localName = "projects")
	static class Projects {
		@JacksonXmlProperty(localName = "project")
		private List<LinkedHashMap<String, String>> projectsListOfMaps;

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			projectsListOfMaps.forEach(p -> {
				s.append(p.toString());
				s.append('\n');
			});
			return s.toString();
		}

	}
}