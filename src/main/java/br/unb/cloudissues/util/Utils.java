package br.unb.cloudissues.util;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public final class Utils {

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private Utils() {

	}

	public static Map<String, Object> responseToMap(String responseBody) {
		Type type = new TypeToken<Map<String, Object>>() {
		}.getType();
		return gson.fromJson(responseBody, type);
	}

	public static List<Map<String, Object>> responseToListOfMap(String responseBody) {
		Type type = new TypeToken<List<Map<String, Object>>>() {
		}.getType();
		return gson.fromJson(responseBody, type);
	}

	/**
	 * Needs the full directory structure to exist.
	 * 
	 * @param obj
	 * @param pathStr
	 * @throws IOException
	 * @throws NoSuchFileException in case any directory does not exist.
	 */
	public static void writeObjToFileAsJSON(Object obj, String pathStr) throws IOException {
		Path path = Paths.get(pathStr);
		String json = gson.toJson(obj);
		Files.write(path, json.getBytes());
	}

	public static <T> List<T> retrieveCollectionFromJSONFile(String path, Class<T> type)
			throws IOException {
		String projectListJSON = String.join("\n", readAllLines(path));
		return gson.fromJson(projectListJSON, new ListOf<T>(type));
	}

	public static <T> List<T> retrieveCollectionFromJSONFileUnchecked(String path, Class<T> type) {
		try {
			return retrieveCollectionFromJSONFile(path, type);
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	public static <T> List<T> retrieveCollectionFromJSONString(String jsonStr, Class<T> type) {
		return gson.fromJson(jsonStr, new ListOf<T>(type));
	}

	public static Map<String, Integer> retrieveRulesFrequencyAsMap(String path) throws IOException {
		String json = String.join("\n", readAllLines(path));
		Type type = new TypeToken<Map<String, Integer>>() {
		}.getType();
		return gson.fromJson(json, type);
	}

	public static String readAllLines(String pathStr) throws IOException {
		List<Charset> charSets = Arrays.asList(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_16,
				StandardCharsets.UTF_8);
		Path path = Paths.get(pathStr);
		for (Charset charset : charSets) {
			try {
				return String.join("\n", Files.readAllLines(path, charset));
			} catch (MalformedInputException e) {

			}
		}
		throw new IllegalStateException("No valid enconding found");
	}

	// @formatter:off
	public static DateTimeFormatter formatterForOffsetDateWithoutCollon() {
		return new DateTimeFormatterBuilder()
				// date/time
				.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				// offset (hh:mm - "+00:00" when it's zero)
				.optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
				// offset (hhmm - "+0000" when it's zero)
				.optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
				// offset (hh - "Z" when it's zero)
				.optionalStart().appendOffset("+HH", "Z").optionalEnd()
				// create formatter
				.toFormatter();
	}
	// @formatter:on

	public static <T> Collection<List<T>> partition(List<T> list, int size) {
		final AtomicInteger counter = new AtomicInteger(0);

		return list.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size))
				.values();
	}

	/**
	 * @return Stream<Path> containing ONLY files (no subdirectories) ending with
	 *         .json
	 * @throws IOException
	 */
	public static Stream<Path> retrieveAllJsonFilesFromDirectory(String directoryPath)
			throws IOException {
		Path path = Paths.get(directoryPath);
		if (!path.toFile().isDirectory()) {
			throw new IllegalArgumentException("directoryPath must be a directory");
		}
		return Files.walk(path).filter(
				p -> !p.toFile().isDirectory() && p.getFileName().toString().endsWith(".json"));
	}

	// @formatter:off
	public static Map<String, Long> reverseFrequencyMapByDescendingValue(Map<String, Long> map) {
		return map.entrySet().parallelStream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(
						Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue,
							(x, y) -> { throw new AssertionError();},
							LinkedHashMap::new
							));
	}
	// @formatter:on

	public static <T> T readJsonFileAndRetrieveClass(String filePath, Class<T> clazz)
			throws IOException {
		return gson.fromJson(readAllLines(filePath), clazz);
	}

	public static String generateJsonPathToSaveForEachProject(String directory,
			String projectName) {
		return directory + sanitizeProjectName(projectName) + "_issues.json";
	}

	public static String sanitizeProjectName(String projectName) {
		String projectNameSanitized = projectName.replaceAll(":", "--");
		projectNameSanitized = projectNameSanitized.replaceAll("/", "---");
		projectNameSanitized = projectNameSanitized.replaceAll("\\s+$", "");
		return projectNameSanitized;
	}

}

class ListOf<T> implements ParameterizedType {
	private final Class<T> type;

	public ListOf(Class<T> type) {
		this.type = type;
	}

	@Override
	public Type[] getActualTypeArguments() {
		return new Type[] { type };
	}

	@Override
	public Type getRawType() {
		return List.class;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}
}
