package ro.linic.ui.http.pojo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;

import javax.net.ssl.SSLSession;

public record StubResponse<T>(int statusCode, T body) implements HttpResponse<T> {
	@Override
	public HttpHeaders headers() {
		return HttpHeaders.of(Collections.emptyMap(), (bi, pr) -> true);
	}

	@Override
	public Optional<HttpResponse<T>> previousResponse() {
		return Optional.empty();
	}

	@Override
	public HttpRequest request() {
		return null;
	}

	@Override
	public Optional<SSLSession> sslSession() {
		return Optional.empty();
	}

	@Override
	public URI uri() {
		return null;
	}

	@Override
	public HttpClient.Version version() {
		return HttpClient.Version.HTTP_2;
	}
}