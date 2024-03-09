package ro.linic.ui.http.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller.PostConfigurer;
import ro.linic.util.commons.ParameterStringBuilder;

public class PostFluent extends RestFluent implements PostConfigurer {
	private BodyProvider body;
	
	public PostFluent(final String url) {
		super(url);
		body = BodyProvider.empty();
	}
	
	@Override
	public PostConfigurer addHeader(final String key, final String value) {
		return (PostConfigurer) super.addHeader(key, value);
	}
	
	@Override
	public PostConfigurer addUrlParam(final String key, final String value) {
		return (PostConfigurer) super.addUrlParam(key, value);
	}

	@Override
	public PostConfigurer body(final BodyProvider body) {
		this.body = Objects.requireNonNull(body);
		return this;
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> async(final BodyHandler<T> responseBodyHandler) {
		final HttpClient client = HttpClient.newBuilder()
		        .version(Version.HTTP_2)
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(30))
		        .build();
		
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url + ParameterStringBuilder.getParamsString(urlParams)))
				.headers(headers.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray(String[]::new))
				.POST(body.get())
				.build();
		return client.sendAsync(request, responseBodyHandler);
	}
}
