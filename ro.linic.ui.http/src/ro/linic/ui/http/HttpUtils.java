package ro.linic.ui.http;

import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import ro.flexbiz.util.commons.HttpStatusCode;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.pojo.Body;
import ro.linic.ui.http.pojo.Result;
import tools.jackson.databind.json.JsonMapper;

public class HttpUtils {
	public final static JsonMapper jsonMapper = new JsonMapper();
	static {
		// UPDATE: this should not be needed in jackson 3.1, because it serializes by default to ISO string format
        // Jackson custom serializers, etc
//        final SimpleModule module = new SimpleModule();
//        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
//        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
//        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
//        jacksonMapper.registerModule(module);
    }
	
	public static <C> C fromJSON(final String json, final Class<C> clazz) {
		if (isEmpty(json))
			return null;
		return jsonMapper.readValue(json, clazz);
	}

	public static List<GenericValue> fromJSON(final String json) {
		if (isEmpty(json))
			return List.of();
		return jsonMapper.readValue(json, Result.class).resultList();
	}
	
	public static String toJSON_Deprecated(final List<GenericValue> items) {
		return jsonMapper.writeValueAsString(new Body(items));
	}
	
	public static String toJSON(final List<GenericValue> items) {
		return jsonMapper.writeValueAsString(items);
	}
	
	public static String toJSON(final GenericValue item) {
		return jsonMapper.writeValueAsString(item);
	}
	
	public static String toJSON(final Map<String, Object> map) {
		return jsonMapper.writeValueAsString(map);
	}

	public static <T> HttpResponse<T> checkOk(final HttpResponse<T> response) {
		if (response.statusCode() != HttpStatusCode.OK.getValue())
			throw new RuntimeException(response + ": " + response.body());
		return response;
	}
}
