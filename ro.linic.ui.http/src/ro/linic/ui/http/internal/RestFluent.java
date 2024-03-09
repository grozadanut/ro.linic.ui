package ro.linic.ui.http.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ro.linic.ui.http.RestCaller.BaseConfigurer;

abstract class RestFluent implements BaseConfigurer {
	protected final String url;
	protected final Map<String, String> headers;
	protected final Map<String, String> urlParams;

	protected RestFluent(final String url) {
		this.url = Objects.requireNonNull(url, "url is required");
		headers = new HashMap<String, String>();
		urlParams = new HashMap<String, String>();
	}

	@Override
	public BaseConfigurer addHeader(final String key, final String value) {
		headers.put(key, value);
		return this;
	}

	@Override
	public BaseConfigurer addUrlParam(final String key, final String value) {
		urlParams.put(key, value);
		return this;
	}
}
