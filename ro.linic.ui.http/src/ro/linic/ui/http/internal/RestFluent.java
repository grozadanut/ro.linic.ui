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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.runtime.ILog;

import ro.flexbiz.util.commons.ParameterStringBuilder;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.HttpHeaders;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller.BaseConfigurer;
import ro.linic.ui.security.model.Authentication;

abstract class RestFluent implements BaseConfigurer {
	private static final ILog log = ILog.of(RestFluent.class);
	
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
		addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		addHeader(HttpHeaders.ACCEPT, "application/json");
		return this;
	}
	
	@Override
	final public <T> CompletableFuture<HttpResponse<T>> asyncRaw(final BodyHandler<T> responseBodyHandler) {
		final HttpClient client = HttpClient.newBuilder()
		        .version(Version.HTTP_2)
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(30))
		        .build();
		
		final String serverUrl = internal ? UIUtils.moquiBaseUrl() : "";
		final Builder reqBuilder = HttpRequest.newBuilder()
		.uri(URI.create(serverUrl + url + ParameterStringBuilder.getParamsString(urlParams)));
		headers.entrySet().forEach(h -> reqBuilder.header(h.getKey(), h.getValue()));
		
		final HttpRequest request = buildMethod(reqBuilder).build();
		return client.sendAsync(request, responseBodyHandler);
	}
	
	@Override
	public CompletableFuture<List<GenericValue>> async(final Consumer<Throwable> exceptionHandler) {
		return asyncRaw(BodyHandlers.ofString())
				.thenApply(HttpUtils::checkOk)
				.thenApply(resp -> HttpUtils.fromJSON(resp.body()))
				.exceptionally(t -> {
					log.error(t.getMessage(), t);
					exceptionHandler.accept(t);
					return List.of();
				});
	}
	
	@Override
	public <T> Optional<T> get(final Class<T> clazz, final Consumer<Throwable> exceptionHandler) {
		try {
			return Optional.ofNullable(asyncRaw(BodyHandlers.ofString())
					.thenApply(HttpUtils::checkOk)
					.thenApply(resp -> HttpUtils.fromJSON(resp.body(), clazz))
					.exceptionally(t -> {
						log.error(t.getMessage(), t);
						exceptionHandler.accept(t);
						return null;
					})
					.get());
		} catch (InterruptedException | ExecutionException e) {
			exceptionHandler.accept(e);
			return Optional.empty();
		}
	}

	protected abstract Builder buildMethod(Builder reqBuilder);
}
