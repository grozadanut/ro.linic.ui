package ro.linic.ui.http.internal;

import java.net.http.HttpRequest.Builder;
import java.util.Objects;

import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller.PostConfigurer;
import ro.linic.ui.security.model.Authentication;

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
	public PostConfigurer internal(final Authentication auth) {
		return (PostConfigurer) super.internal(auth);
	}
	
	@Override
	public PostConfigurer body(final BodyProvider body) {
		this.body = Objects.requireNonNull(body);
		return this;
	}

	@Override
	protected Builder buildMethod(final Builder reqBuilder) {
		return reqBuilder.POST(body.get());
	}
}
