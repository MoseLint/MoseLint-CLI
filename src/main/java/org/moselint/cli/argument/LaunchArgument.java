package org.moselint.cli.argument;

import java.util.Optional;

public interface LaunchArgument<I> {

	String getArgumentTag();

	Optional<I> getValue();

	I parseValue(String value);


}
