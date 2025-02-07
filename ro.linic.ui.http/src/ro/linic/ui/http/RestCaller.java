package ro.linic.ui.http;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import ro.linic.ui.http.internal.GetFluent;
import ro.linic.ui.http.internal.PostFluent;
import ro.linic.ui.security.model.Authentication;

public interface RestCaller {

	interface BaseConfigurer {
		BaseConfigurer addHeader(String key, String value);
		BaseConfigurer addUrlParam(String key, String value);
		/**
		 * Specify whether this is an internal call. If true, you can omit 
		 * the server URL. eg: get("/products/1")
		 */
		BaseConfigurer internal(Authentication auth);
		<T> CompletableFuture<HttpResponse<T>> async(BodyHandler<T> responseBodyHandler);
		/**
		 * Convenience method to check the response and convert the returned JSON 
		 * to your specified class. On any status other than 200 or any other error 
		 * calls the exception handler.
		 * 
		 * @param clazz the class that the JSON will be deserialized into
		 * @param exceptionHandler if any error occurs this will be called to handle the exception
		 * @return the response deserialized from JSON or empty when error(blocking call)
		 */
		<T> Optional<T> get(Class<T> clazz, Consumer<Throwable> exceptionHandler);
	}
	
	interface PostConfigurer extends BaseConfigurer {
		@Override PostConfigurer addHeader(String key, String value);
		@Override PostConfigurer addUrlParam(String key, String value);
		PostConfigurer body(BodyProvider body);
    }
	
	public static PostConfigurer post(final String url) {
        return new PostFluent(url);
    }
	
	public static BaseConfigurer get(final String url) {
        return new GetFluent(url);
    }
}
