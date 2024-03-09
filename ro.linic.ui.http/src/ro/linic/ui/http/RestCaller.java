package ro.linic.ui.http;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;

import ro.linic.ui.http.internal.PostFluent;

public interface RestCaller {

	interface BaseConfigurer {
		BaseConfigurer addHeader(String key, String value);
		BaseConfigurer addUrlParam(String key, String value);
	}
	
	interface PostConfigurer extends BaseConfigurer {
		@Override PostConfigurer addHeader(String key, String value);
		@Override PostConfigurer addUrlParam(String key, String value);
		PostConfigurer body(BodyProvider body);
		<T> CompletableFuture<HttpResponse<T>> async(BodyHandler<T> responseBodyHandler);
    }
	
	public static PostConfigurer post(final String url) {
        return new PostFluent(url);
    }
}
