package org.moselint.cli.argument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntFunction;

public class DependenciesArgument implements LaunchArgument<File[]>{

    private File[] dependency;
    @Override
    public String getArgumentTag() {
        return "-d";
    }

    @Override
    public Optional<File[]> getValue() {
        return Optional.ofNullable(this.dependency);
    }

    @Override
    public File[] parseValue(String value) {
        if(value.contains(",")){
            this.dependency = Arrays.stream(value.split(",")).map(File::new).filter(File::exists).filter(File::canRead).filter(file -> file.getName().endsWith(".jar")).toArray(File[]::new);
        return this.dependency;
        }
        File file = new File(value);
        if(!file.exists()){
            throw new IllegalArgumentException("File/Folder of '" + value + "' cannot be found");
        }
        if(!file.canRead()){
            throw new IllegalArgumentException("File/Folder of '" + value + "' does not have read permissions");
        }
        if(file.isFile()) {
            if(!file.getName().endsWith(".jar")){
                throw new IllegalArgumentException("File of '" + value + "' is not a .jar file");
            }
            this.dependency = new File[]{file};
            return this.dependency;
        }
        try {
            this.dependency = Files.walk(file.toPath()).map(Path::toFile).filter(File::isFile).filter(File::canRead).filter(walkFile -> walkFile.getName().endsWith(".jar")).toArray(File[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this.dependency;
    }
}
