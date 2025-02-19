package ro.linic.ui.http.internal;

import java.net.http.HttpRequest.Builder;
import java.util.Objects;

import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller.PutConfigurer;
import ro.linic.ui.security.model.Authentication;

public class PutFluent extends RestFluent implements PutConfigurer {
	private BodyProvider body;
	
	public PutFluent(final String url) {
		super(url);
		body = BodyProvider.empty();
	}
	
	@Override
	public PutConfigurer addHeader(final String key, final String value) {
		return (PutConfigurer) super.addHeader(key, value);
	}
	
	@Override
	public PutConfigurer addUrlParam(final String key, final String value) {
		return (PutConfigurer) super.addUrlParam(key, value);
	}
	
	@Override
	public PutConfigurer internal(final Authentication auth) {
		return (PutConfigurer) super.internal(auth);
	}
	
	@Override
	public PutConfigurer body(final BodyProvider body) {
		this.body = Objects.requireNonNull(body);
		return this;
	}

	@Override
	protected Builder buildMethod(final Builder reqBuilder) {
		return reqBuilder.PUT(body.get());
	}
}
