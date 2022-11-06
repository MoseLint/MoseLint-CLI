package org.moselint.cli.argument;

import java.io.File;
import java.util.Optional;

public class SourceArgument implements LaunchArgument<File> {

	private File file;

	@Override
	public String getArgumentTag() {
		return "-s";
	}

	@Override
	public Optional<File> getValue() {
		return Optional.ofNullable(this.file);
	}

	@Override
	public File parseValue(String value) {
		this.file = new File(value);
		return this.file;
	}
}
