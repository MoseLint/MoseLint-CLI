package org.moselint.cli.argument.self;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openblock.creator.code.function.IFunction;
import org.openblock.creator.impl.custom.clazz.AbstractCustomClass;
import org.openblock.creator.impl.custom.clazz.reader.CustomClassReader;
import org.openblock.creator.project.Project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LaunchArgumentSelfTests {

	private static AbstractCustomClass customClass;

	@BeforeAll
	public static void init() throws IOException {
		File self = new File("src/main/java");
		Assumptions.assumeTrue(self.exists());


		List<File> files = Files.walk(self.toPath())
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
		CustomClassReader reader = null;
		for (Map.Entry<CustomClassReader, AbstractCustomClass> entry : classesStageOne.entrySet()) {
			if (entry.getValue().getName().equals("LaunchArgument")
					&& entry.getValue().getPackage().length == 4
					&& entry.getValue().getPackage()[3].equals("argument")) {
				customClass = entry.getValue();
				reader = entry.getKey();
			}
			project.register(entry.getValue());
		}
		if (reader == null) {
			throw new RuntimeException("Could not find class of org.moselint.cli.argument.LaunchArgument");
		}

		for (Map.Entry<CustomClassReader, AbstractCustomClass> entry : classesStageOne.entrySet()) {
			entry.getKey().readStageTwo(project, entry.getValue());
		}

		customClass = reader.readStageThree(project, customClass);
	}

	@Test
	public void testGenerics() {
		Assertions.assertEquals(1, customClass.getGenerics().size());
		Assertions.assertEquals("I", customClass.getGenerics().get(0).getName());
	}

	@Test
	public void functionTest() {
		Assertions.assertEquals(3, customClass.getFunctions().size());
		Iterator<IFunction> iter = customClass.getFunctions().iterator();
		IFunction getArgumentTag = iter.next();
		IFunction getValue = iter.next();
		IFunction parseValue = iter.next();


		Assertions.assertEquals("getArgumentTag", getArgumentTag.getName());
		Assertions.assertEquals("getValue", getValue.getName());
		Assertions.assertEquals("parseValue", parseValue.getName());

		Assertions.assertEquals("String", getArgumentTag.getReturnType().getDisplayText());
		Assertions.assertEquals("I", parseValue.getReturnType().getDisplayText());
		Assertions.assertEquals("Optional<I>", getValue.getReturnType().getDisplayText());

	}
}
