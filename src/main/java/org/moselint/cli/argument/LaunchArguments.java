package org.moselint.cli.argument;

import java.util.Collection;
import java.util.List;

public class LaunchArguments {

	public static final SourceArgument SOURCE = new SourceArgument();
	public static final DependenciesArgument DEPENDENCIES = new DependenciesArgument();

	public static Collection<LaunchArgument<?>> getLaunchArguments() {
		return List.of(SOURCE, DEPENDENCIES);
	}
}
