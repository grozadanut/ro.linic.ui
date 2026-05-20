package ro.linic.ui.http.internal;

import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILog;

import ro.flexbiz.util.commons.ParameterStringBuilder;
import ro.flexbiz.util.commons.StringUtils;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.HttpHeaders;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller.BaseConfigurer;
import ro.linic.ui.http.pojo.StubResponse;
import ro.linic.ui.security.services.AuthenticationSession;

abstract class RestFluent implements BaseConfigurer {
	private static final ILog log = ILog.of(RestFluent.class);
	
	protected final String url;
	protected final Map<String, String> headers;
	protected final Map<String, String> urlParams;
	protected boolean internal;
	protected AuthenticationSession session;

	protected RestFluent(final String url) {
		this.url = Objects.requireNonNull(url, "url is required");
		headers = new HashMap<String, String>();
		urlParams = new HashMap<String, String>();
		internal = false;
	}

	@Override
	public BaseConfigurer addHeader(final String key, final String value) {
		if (value != null)
			headers.put(key, value);
		return this;
	}

	@Override
	public BaseConfigurer addUrlParam(final String key, final String value) {
		if (value != null)
			urlParams.put(key, value);
		return this;
	}
	
	@Override
	public BaseConfigurer internal(final AuthenticationSession session) {
		this.internal = true;
		this.session = Objects.requireNonNull(session);
		addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		addHeader(HttpHeaders.ACCEPT, "application/json");
		if (!StringUtils.isEmpty(session.authentication().getSessionId()))
			addHeader("Cookie", "JSESSIONID="+session.authentication().getSessionId());
		addHeader("x-csrf-token", session.authentication().getCsrf());
		addHeader("moquisessiontoken", session.authentication().getCsrf());
		return this;
	}
	
	@Override
	final public <T> CompletableFuture<HttpResponse<T>> asyncRaw(final BodyHandler<T> responseBodyHandler) {
		final HttpClient client = HttpClient.newBuilder()
		        .version(Version.HTTP_2)
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(30))
		        .build();
		
		if (internal && isEmpty(UIUtils.moquiBaseUrl()))
			return CompletableFuture.completedFuture(new StubResponse<>(200, null));

		return client.sendAsync(buildRequest(), responseBodyHandler)
				.thenCompose(response -> {
					if (internal && response.statusCode() == 403 && (response.body()+"").contains("User [No User] is not authorized for View on REST Path"))
						return retryRequestAfterRelogin(client, responseBodyHandler);
					return CompletableFuture.completedFuture(response);
				});
	}
	
	private HttpRequest buildRequest() {
		final String serverUrl = internal ? UIUtils.moquiBaseUrl() : "";
		final Builder reqBuilder = HttpRequest.newBuilder()
				.uri(URI.create(serverUrl + url + ParameterStringBuilder.getParamsString(urlParams)));
		headers.entrySet().forEach(h -> reqBuilder.header(h.getKey(), h.getValue()));
		return buildMethod(reqBuilder).build();
	}
	
	private <T> CompletionStage<HttpResponse<T>> retryRequestAfterRelogin(final HttpClient client, final BodyHandler<T> responseBodyHandler) {
		session.invalidate();
		internal(session); // relogin and refresh session tokens
		final HttpRequest request = buildRequest();
		log.info("session refreshed, now retrying: "+request);
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
	public <T> Optional<T> sync(final Class<T> clazz, final Consumer<Throwable> exceptionHandler) {
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
	
	@Override
	public List<GenericValue> sync(final Consumer<Throwable> exceptionHandler) {
		try {
			return async(exceptionHandler).get();
		} catch (InterruptedException | ExecutionException e) {
			exceptionHandler.accept(e);
			return List.of();
		}
	}
	
	@Override
	final public <T> Optional<HttpResponse<T>> syncRaw(final BodyHandler<T> responseBodyHandler, final Consumer<Throwable> exceptionHandler) {
		try {
			return Optional.ofNullable(asyncRaw(responseBodyHandler)
					.thenApply(HttpUtils::checkOk)
					.exceptionally(t -> {
						log.error(t.getMessage(), t);
						exceptionHandler.accept(t);
						return null;
					}).get());
		} catch (InterruptedException | ExecutionException e) {
			exceptionHandler.accept(e);
			return Optional.empty();
		}
	}

	protected abstract Builder buildMethod(Builder reqBuilder);
}
