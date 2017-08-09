package ru.javaops.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class JsonUtil {

    private static ObjectMapper mapper;

    public static <T> List<T> readValues(String json, Class<T> clazz) {
        ObjectReader reader = mapper.readerFor(clazz);
        try {
            return reader.<T>readValues(json).readAll();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid read array from JSON:\n'" + json + "'", e);
        }
    }

    public static <T> List<T> readValues(Reader reader, Class<T> clazz) {
        ObjectReader objectReader = mapper.readerFor(clazz);
        try {
            return objectReader.<T>readValues(reader).readAll();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid read array from reader", e);
        }
    }

    public static <T> T readValue(Reader reader, TypeReference<T> typeRef) {
        try {
            return mapper.readValue(reader, typeRef);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid read from reader", e);
        }
    }

    public static <T> T readValue(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid read from JSON:\n'" + json + "'", e);
        }
    }

    public static <T> String writeValue(T obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid write to JSON:\n'" + obj + "'", e);
        }
    }

    public static <T> String writeValue(T obj, ObjectWriter ow) {
        try {
            return ow.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid write to JSON:\n'" + obj + "'", e);
        }
    }

    public static void setMapper(ObjectMapper mapper) {
        JsonUtil.mapper = mapper;
    }
}
