package ro.linic.ui.http;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.internal.GetFluent;
import ro.linic.ui.http.internal.PostFluent;
import ro.linic.ui.http.internal.PutFluent;
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
		<T> CompletableFuture<HttpResponse<T>> asyncRaw(BodyHandler<T> responseBodyHandler);
		/**
		 * Convenience method to check the response and convert the returned JSON 
		 * to a list of GenericValues. On any status other than 200 or any other error 
		 * calls the exception handler.
		 * 
		 * @param exceptionHandler if any error occurs this will be called to handle the exception
		 * @return the response deserialized from JSON or empty list when error
		 */
		CompletableFuture<List<GenericValue>> async(Consumer<Throwable> exceptionHandler);
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
		@Override PostConfigurer internal(Authentication auth);
		PostConfigurer body(BodyProvider body);
    }
	
	interface PutConfigurer extends BaseConfigurer {
		@Override PutConfigurer addHeader(String key, String value);
		@Override PutConfigurer addUrlParam(String key, String value);
		@Override PutConfigurer internal(Authentication auth);
		PutConfigurer body(BodyProvider body);
    }
	
	public static PostConfigurer post(final String url) {
        return new PostFluent(url);
    }
	
	public static BaseConfigurer get(final String url) {
        return new GetFluent(url);
    }
	
	public static PutConfigurer put(final String url) {
        return new PutFluent(url);
    }
}
