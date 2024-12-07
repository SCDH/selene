package de.wwu.scdh.annotations.selections.service.impl;

import java.io.InputStream;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.FormParam;


import org.jboss.resteasy.reactive.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotations.selections.service.api.WadmApi;
import de.wwu.scdh.annotations.selections.service.model.Config;

@RequestScoped
public class WadmImpl implements WadmApi {

    public static final Logger LOG = LoggerFactory.getLogger(WadmImpl.class);

    @Inject
    HttpServerRequest request;

    @Override
    public Response wadmNormalizePost(InputStream modelInputStream, InputStream sourceInputStream, Config config) {
	LOG.info("request header: Accept: {}", request.getHeader("Accept"));
	return RestResponse.ok("").toResponse();
    }

}

