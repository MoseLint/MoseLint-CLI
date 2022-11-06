package org.moselint.cli;

import org.moselint.MoseLint;
import org.moselint.check.Checker;
import org.moselint.check.Checkers;
import org.moselint.cli.argument.LaunchArgument;
import org.moselint.cli.argument.LaunchArguments;
import org.moselint.exception.CheckException;
import org.moselint.exception.CheckExceptionContext;
import org.openblock.creator.code.Codeable;
import org.openblock.creator.impl.custom.clazz.AbstractCustomClass;
import org.openblock.creator.impl.custom.clazz.reader.CustomClassReader;
import org.openblock.creator.project.Project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CliMain {

	public static void main(String[] args) throws IOException {
		LaunchArgument<?> argument = null;
		for (String target : args) {
			if (argument != null) {
				argument.parseValue(target);
				argument = null;
				continue;
			}

			Optional<LaunchArgument<?>> opArgument = LaunchArguments.getLaunchArguments()
					.stream()
					.filter(arg -> target.equalsIgnoreCase(arg.getArgumentTag()))
					.findAny();
			if (opArgument.isEmpty()) {
				System.err.println("Unknown argument of '" + target + "'");
				return;
			}
			argument = opArgument.get();
		}

		if (LaunchArguments.SOURCE.getValue().isEmpty()) {
			System.err.println("Missing required argument of source '" + LaunchArguments.SOURCE.getArgumentTag() +
					"'");
			return;
		}

		File source = LaunchArguments.SOURCE.getValue().get();

		System.out.println("Reading: " + source.getAbsolutePath());
		if (!source.exists()) {
			System.err.println("Specified path does not exist");
			return;
		}

		Collection<AbstractCustomClass> classes = readClasses();
		System.out.println("Found " + classes.size() + " class files");
		Collection<Checker> checks = Checkers.getCheckers();
		classes.forEach(clazz -> {
			Map<Checker, CheckException> map = MoseLint.checkForAll(clazz, checks);
			System.out.println("Class: " + String.join(".", clazz.getPackage()) + "." + clazz.getName());
			map.forEach((checker, exception) -> {
				System.out.println("\tCheck: " + checker.getDisplayName());
				for (CheckExceptionContext context : exception.getContext()) {
					System.out.println("\t\tIssue: " + context.getMessage());
					for (Codeable codeable : context.getErrors()) {
						System.out.println("\t\t\tIssue caused by: " + codeable.writeCode(0));
					}
					for (Codeable codeable : context.getSuggestions()) {
						System.out.println("\t\t\tSuggestions: " + codeable.writeCode(0));
					}

				}
			});
		});

	}

	private static Collection<AbstractCustomClass> readClasses() throws IOException {
		if (LaunchArguments.SOURCE.getValue().isEmpty()) {
			throw new IOException("Source not found");
		}
		List<File> files = Files.walk(LaunchArguments.SOURCE.getValue().get().toPath())
				.filter(path -> {
					Path fileName = path.getFileName();
					return fileName.toString().endsWith(".java");
				})
				.map(Path::toFile)
				.toList();
		Project<AbstractCustomClass> project = new Project<>("Lint");

		Map<CustomClassReader, AbstractCustomClass> classesStageOne = files.parallelStream().map(file -> {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				List<String> classLines = br.lines().toList();
				CustomClassReader ccr = new CustomClassReader(classLines);
				return Map.entry(ccr, ccr.readStageOne(project));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		for(AbstractCustomClass clazz : classesStageOne.values()){
			project.register(clazz);
		}

		Map<CustomClassReader, AbstractCustomClass> classesStageTwo =
				classesStageOne.entrySet().parallelStream().map(entry -> {
					try {
						AbstractCustomClass acc = entry.getKey().readStageTwo(project, entry.getValue());
						return Map.entry(entry.getKey(), acc);
					} catch (Throwable e) {
						System.err.println("Error reading " + entry.getValue().getFullName());
						throw e;
					}
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<CustomClassReader, AbstractCustomClass> classesStageThree =
				classesStageTwo.entrySet().parallelStream().map(entry -> {
					AbstractCustomClass acc = entry.getKey().readStageThree(project, entry.getValue());
					return Map.entry(entry.getKey(), acc);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return classesStageThree.values();
	}
}
