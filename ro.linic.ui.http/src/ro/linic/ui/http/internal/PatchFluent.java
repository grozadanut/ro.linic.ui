package ro.linic.ui.http.internal;

import java.net.http.HttpRequest.Builder;
import java.util.Objects;

import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller.PatchConfigurer;
import ro.linic.ui.security.model.Authentication;

public class PatchFluent extends RestFluent implements PatchConfigurer {
	private BodyProvider body;
	
	public PatchFluent(final String url) {
		super(url);
		body = BodyProvider.empty();
	}
	
	@Override
	public PatchConfigurer addHeader(final String key, final String value) {
		return (PatchConfigurer) super.addHeader(key, value);
	}
	
	@Override
	public PatchConfigurer addUrlParam(final String key, final String value) {
		return (PatchConfigurer) super.addUrlParam(key, value);
	}
	
	@Override
	public PatchConfigurer internal(final Authentication auth) {
		return (PatchConfigurer) super.internal(auth);
	}
	
	@Override
	public PatchConfigurer body(final BodyProvider body) {
		this.body = Objects.requireNonNull(body);
		return this;
	}

	@Override
	protected Builder buildMethod(final Builder reqBuilder) {
		return reqBuilder.method("PATCH", body.get());
	}
}
