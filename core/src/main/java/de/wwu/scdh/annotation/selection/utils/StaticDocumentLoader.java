package de.wwu.scdh.annotation.selection.utils;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.FileLoader;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link StaticDocumentLoader} is a {@link DocumentLoader} that returns local assets instead of remote one.
 * It is configured with a resource mapping that maps URIs to local assets. Its purpose is to speed up
 * JSON-LD processing with version pinned contexts, as for Web Annotations.<P/>
 *
 * The loader delegated to a fallback loader when the requested URI is not in the resource mapping.<P/>
 *
 * The structure of the resource mapping JSON file:
 *
 * <pre>
 *     {
 *       "https://www.w3.org/ns/anno.jsonld": {
 *         "path": "anno.jsonld"
 *       },
 *       "http://www.w3.org/ns/anno.jsonld": {
 *         "path": "anno.jsonld"
 *       }
 *     }
 * </pre>
 */
public class StaticDocumentLoader implements DocumentLoader {

	private static final Logger LOG = LoggerFactory.getLogger(StaticDocumentLoader.class);

	/**
	 * A default context mapping resource.
	 */
	public static final URL CONTEXT_MAPPING = StaticDocumentLoader.class.getResource("/context/context-map.json");

	private final DocumentLoader fallbackLoader;

	private final Map<URI, URI> contextMapping;

	private final boolean delegateOnError;

	/**
	 * Creates a new {@link StaticDocumentLoader} from a resource mapping given by {@link File}.
	 * @param contextMap - a JSON {@link File>} with the resource mapping
	 * @param fallbackLoader - a {@link DocumentLoader} used to handle request for non-mapped resources
	 * @param delegateOnError - whether to delegate the request to the fallback, when loading of a local asset failed.
	 */
	public StaticDocumentLoader(final File contextMap, final DocumentLoader fallbackLoader, boolean delegateOnError) {
		this.fallbackLoader = fallbackLoader;
		this.delegateOnError = delegateOnError;
		contextMapping = setup(contextMap);
	}

	/**
	 * Creates a new {@link StaticDocumentLoader} from the {@link StaticDocumentLoader#CONTEXT_MAPPING}.
	 */
	public StaticDocumentLoader() {
		fallbackLoader = (new JsonLdOptions()).getDocumentLoader(); // default loader
		delegateOnError = true;
		File defaultContextMapping = new File(CONTEXT_MAPPING.getPath());
		contextMapping = setup(defaultContextMapping);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
		if (contextMapping.containsKey(url)) {
			URI file = contextMapping.get(url);
			FileLoader fileLoader = new FileLoader();
			try {
				return fileLoader.loadDocument(file, options);
			} catch (JsonLdError e) {
				LOG.error("failed to load static asset for {}: {}", url, e.getMessage());
				if (delegateOnError) {
					return fallbackLoader.loadDocument(url, options);
				} else {
					throw new JsonLdError(e.getCode(), e.getMessage());
				}
			}
		} else {
			// delegate to fallback loader
			return fallbackLoader.loadDocument(url, options);
		}
	}

	private Map<URI, URI> setup(File contextMap) {
		Path path = contextMap.toPath().getParent();
		Map<URI, URI> assets = new HashMap<>();

		try {
			JsonReader jsonReader = Json.createReader(new FileReader(contextMap));
			JsonStructure jsonStructure = jsonReader.read();
			JsonObject root = jsonStructure.asJsonObject();
			for (String url : root.keySet()) {
				try {
					URI remote = new URI(url);
					String relative = root.get(url).asJsonObject().getString("path");
					File resolved = path.resolve(relative).toFile();
					if (resolved.isFile()) {
						assets.put(remote, resolved.toURI());
					} else {
						LOG.error(
								"context map entry {} configures file {}, which resolves to {}. File not present",
								url,
								relative,
								resolved);
					}
				} catch (URISyntaxException e) {
					LOG.error("context map entry {} is not a valid URI. Continuing without this entry", url);
				} catch (Exception e) {
					LOG.error("invalid context map entry {}. Continuing without this entry", url);
				}
			}
		} catch (FileNotFoundException e) {
			LOG.error("file not found: {}\nContinuing without context map", contextMap);
		}
		return Map.copyOf(assets); // makes map unmodifiable
	}

	/**
	 * Tells whether this instance has a local version for a URI.
	 * @param uri - the remote URI
	 * @return - <code>true</code> if a local version is available.
	 */
	public boolean hasLocal(URI uri) {
		return contextMapping.containsKey(uri);
	}
}
