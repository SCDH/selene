package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

/**
 * This is the main entry point of the command line interface. The
 * app *{app.name}* has subcommands.
 */
@Command(name = "${app.name}",
	 mixinStandardHelpOptions = true,
	 version = "${revision}${changelist}",
	 subcommands = {
	     Normalize.class,
	     NormalizeWADM.class
	 })
public class App {

    public static void main(String... args) {
	System.exit(new CommandLine(new App()).execute(args));
    }

}
