package ro.linic.ui.http.internal;

import java.net.http.HttpRequest.Builder;

import ro.linic.ui.http.RestCaller.BaseConfigurer;

public class GetFluent extends RestFluent implements BaseConfigurer {
	
	public GetFluent(final String url) {
		super(url);
	}
	
	@Override
	protected Builder buildMethod(final Builder reqBuilder) {
		return reqBuilder.GET();
	}
}
