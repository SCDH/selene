package de.wwu.scdh.annotations.selections.service.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import de.wwu.scdh.annotations.selections.service.model.Config;


/**
 * The converter provided by this class is required for the
 * {@link Config} passed in as a part of multipart
 * form-data.
 */
@Provider
public class ConfigParamConverterProvider implements ParamConverterProvider {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter
        (Class<T> rawType,
         Type genericType,
         Annotation[] annotations) {
        if (rawType.isAssignableFrom(Config.class)) {
            return (ParamConverter<T>) new ConfigParamConverter();
        }
        return null;
    }

}
