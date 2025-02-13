package ro.linic.ui.http;

import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.pojo.Result;
import ro.linic.util.commons.HttpStatusCode;

public class HttpUtils {
	public static <C> C fromJSON(final String json, final Class<C> clazz) {
		try {
			return new ObjectMapper().readValue(json, clazz);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<GenericValue> fromJSON(final String json) {
		try {
			return new ObjectMapper().readValue(json, Result.class).resultList();
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
