package ro.linic.ui.http.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import ro.linic.ui.base.preferences.PreferenceKey;
import ro.linic.ui.http.HttpHeaders;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller.BaseConfigurer;
import ro.linic.ui.security.model.Authentication;
import ro.linic.util.commons.ParameterStringBuilder;

abstract class RestFluent implements BaseConfigurer {
	protected final String url;
	protected final Map<String, String> headers;
	protected final Map<String, String> urlParams;
	protected boolean internal;

	protected RestFluent(final String url) {
		this.url = Objects.requireNonNull(url, "url is required");
		headers = new HashMap<String, String>();
		urlParams = new HashMap<String, String>();
		internal = false;
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
	
	@Override
	public BaseConfigurer internal(final Authentication auth) {
		this.internal = true;
		final String credentials = auth.getName() + ":" + auth.getCredentials();
		final byte[] encodedAuth = Base64.encodeBase64(credentials.getBytes(StandardCharsets.ISO_8859_1));
		final String authHeader = "Basic " + new String(encodedAuth);
		addHeader(HttpHeaders.AUTHORIZATION, authHeader);
		return this;
	}
	
	@Override
	final public <T> CompletableFuture<HttpResponse<T>> async(final BodyHandler<T> responseBodyHandler) {
		final HttpClient client = HttpClient.newBuilder()
		        .version(Version.HTTP_2)
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(30))
		        .build();
		
		final String serverUrl = internal ? findServerBaseUrl() : "";
		final Builder reqBuilder = HttpRequest.newBuilder()
		.uri(URI.create(serverUrl + url + ParameterStringBuilder.getParamsString(urlParams)));
		headers.entrySet().forEach(h -> reqBuilder.header(h.getKey(), h.getValue()));
		
		final HttpRequest request = buildMethod(reqBuilder).build();
		return client.sendAsync(request, responseBodyHandler);
	}
	
	@Override
	public <T> Optional<T> get(final Class<T> clazz, final Consumer<Throwable> exceptionHandler) {
		try {
			return Optional.ofNullable(async(BodyHandlers.ofString())
					.thenApply(HttpUtils::checkOk)
					.thenApply(resp -> HttpUtils.readJson(resp.body(), clazz))
					.exceptionally(t -> {
						exceptionHandler.accept(t);
						return null;
					})
					.get());
		} catch (InterruptedException | ExecutionException e) {
			exceptionHandler.accept(e);
			return Optional.empty();
		}
	}

	private String findServerBaseUrl() {
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode("ro.linic.ui.base");
		return prefs.get(PreferenceKey.SERVER_BASE_URL, PreferenceKey.SERVER_BASE_URL_DEF);
	}

	protected abstract Builder buildMethod(Builder reqBuilder);
}
