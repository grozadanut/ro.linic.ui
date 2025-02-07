package ro.linic.ui.http;

import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.linic.util.commons.HttpStatusCode;

public class HttpUtils {
	public static <C> C readJson(final String json, final Class<C> clazz) {
		try {
			return new ObjectMapper().readValue(json, clazz);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> HttpResponse<T> checkOk(final HttpResponse<T> response) {
		if (response.statusCode() != HttpStatusCode.OK.getValue())
			throw new RuntimeException(response+": "+response.body());
		return response;
	}
}
