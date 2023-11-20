package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

@Command(name = "${app.name}", mixinStandardHelpOptions = true, version = "${revision}${changelist}")
public class SelectionEngine implements Callable<Integer> {

    enum DOMParser {
	XML_PARSER,
	HTML_PARSER
    }

    enum Normalizer {
	FROM_ROOT_CLARK,
	FROM_DEEPEST_ID_CLARK
    }

    public static final String DEFAULT_NORMALIZER = Normalizer.FROM_DEEPEST_ID_CLARK.name();

    @Parameters(paramLabel = "RESOURCE",
		description = "The file the selector selects from")
    URI resource;

    @Option(names = { "-p", "--parser" },
	    paramLabel = "PARSER",
	    defaultValue = "${DOM_PARSER:DOMParser.XML_PARSER.name()}",
	    description = "The parser used for reading the RESOURCE. Valid values: ${COMPLETION-CANDIDATES}")
    DOMParser parser;

    @Option(names = { "-x", "--xpath" },
	    paramLabel = "XPATH",
	    description = "the XPath value of an XPath selector")
    String xpath;

    @Option(names = { "-c", "--character" },
	    paramLabel = "CHAR",
	    description = "the character-scheme position the XPath selector is refined with")
    int character;

    @Option(names = { "-n", "--normalizer" },
	    paramLabel = "NORMALIZER",
	    defaultValue = "${NORMALIZER:DEFAULT_NORMALIZER}",
	    description = "The normalizer for XPath part of the selector. Valid values: ${COMPLETION-CANDIDATES}")
    Normalizer normalizer;

    @Override
    public Integer call() throws Exception {
	System.out.printf("Hello");
	return 0;
    }

    public static void main(String... args) {
	System.exit(new CommandLine(new SelectionEngine()).execute(args));
    }

}
