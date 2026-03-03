package ro.linic.ui.http;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import ro.flexbiz.util.commons.HttpStatusCode;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.pojo.Body;
import ro.linic.ui.http.pojo.Result;

public class HttpUtils {
	public final static ObjectMapper jacksonMapper = new ObjectMapper();
	static {
        // Jackson custom serializers, etc
        final SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
        jacksonMapper.registerModule(module);
    }
	
	public static <C> C fromJSON(final String json, final Class<C> clazz) {
		try {
			return jacksonMapper.readValue(json, clazz);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<GenericValue> fromJSON(final String json) {
		try {
			return jacksonMapper.readValue(json, Result.class).resultList();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String toJSON_Deprecated(final List<GenericValue> items) {
		try {
			return jacksonMapper.writeValueAsString(new Body(items));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String toJSON(final List<GenericValue> items) {
		try {
			return jacksonMapper.writeValueAsString(items);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String toJSON(final GenericValue item) {
		try {
			return jacksonMapper.writeValueAsString(item);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String toJSON(final Map<String, Object> map) {
		try {
			return jacksonMapper.writeValueAsString(map);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> HttpResponse<T> checkOk(final HttpResponse<T> response) {
		if (response.statusCode() != HttpStatusCode.OK.getValue())
			throw new RuntimeException(response + ": " + response.body());
		return response;
	}
}
