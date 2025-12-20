package ro.linic.ui.http.internal;

import java.net.http.HttpRequest.Builder;

import ro.linic.ui.http.RestCaller.DeleteConfigurer;
import ro.linic.ui.security.model.Authentication;

public class DeleteFluent extends RestFluent implements DeleteConfigurer {
	
	public DeleteFluent(final String url) {
		super(url);
	}
	
	@Override
	public DeleteConfigurer addHeader(final String key, final String value) {
		return (DeleteConfigurer) super.addHeader(key, value);
	}
	
	@Override
	public DeleteConfigurer addUrlParam(final String key, final String value) {
		return (DeleteConfigurer) super.addUrlParam(key, value);
	}
	
	@Override
	public DeleteConfigurer internal(final Authentication auth) {
		return (DeleteConfigurer) super.internal(auth);
	}
	
	@Override
	protected Builder buildMethod(final Builder reqBuilder) {
		return reqBuilder.DELETE();
	}
}
