package ro.linic.ui.legacy.session;

import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.linic.ui.legacy.session.UIUtils.readJsonProperty;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wildfly.common.annotation.Nullable;

public class RestCaller
{
	public static final String API_ADDRESS_KEY = "api_address";
	public static final String API_ADDRESS_DEFAULT = "http://localhost:8080/api";
	
	public static String post_InternalApi(final String apiSuffix, final Optional<String> json)
			throws UnsupportedOperationException, IOException
	{
		final String auth = ClientSession.instance().getUsername() + ":" + ClientSession.instance().getPassword();
		final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
		final String authHeader = "Basic " + new String(encodedAuth);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create()
				.build())
		{
			final String baseAddress = System.getProperty(API_ADDRESS_KEY, API_ADDRESS_DEFAULT);
			final HttpPost request = new HttpPost(baseAddress+apiSuffix);
			if (json.isPresent())
			{
				final StringEntity params = new StringEntity(json.get());
				request.setEntity(params);
			}
			request.addHeader("content-type", "application/json");
			request.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
			final HttpResponse response = httpClient.execute(request);
			final String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.toString());
			response.getEntity().getContent().close();
			
			if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode())
				throw new IOException("STATUS: " + response.getStatusLine().getStatusCode() + SPACE +
						response.getStatusLine().getReasonPhrase());
				
		    return result;
		}
	}
	
	public static Optional<HttpResponseW> get(final String url)
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create()
				.build())
		{
			final HttpGet request = new HttpGet(url);
		    return HttpResponseW.wrap(httpClient.execute(request));
		}
		catch (final ClientProtocolException e)
		{
			e.printStackTrace();
			showException(e);
			return Optional.empty();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			showException(e);
			return Optional.empty();
		}
	}
	
	public static <T> Collection<T> get_Json_WithSSL(final String url, final List<NameValuePair> params, final Class<T> clazz,
			@Nullable final UISynchronize sync)
	{
		try {
			final SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();

			try (CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLContext(sslContext)
					.build())
			{
				final HttpGet request = new HttpGet(url);
				request.addHeader("content-type", "application/json");
				final URI uri = new URIBuilder(request.getURI()).addParameters(params).build();
				request.setURI(uri);
				
				final HttpResponse response = httpClient.execute(request);
				final int statusCode = response.getStatusLine().getStatusCode();
				if (HttpStatus.SC_OK != statusCode)
				{
					final String entity = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.toString());
					final String reasonPhrase = readJsonProperty(entity, "message")
							.orElse(response.getStatusLine().getReasonPhrase());
					System.err.println(entity);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "ERROR "+url, reasonPhrase);
					return List.of();
				}
				
				return UIUtils.readJsonList(response.getEntity().getContent(), clazz);
			}
			catch (final ClientProtocolException e)
			{
				e.printStackTrace();
				showException(e, sync);
				return List.of();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				showException(e, sync);
				return List.of();
			}
			catch (final URISyntaxException e)
			{
				e.printStackTrace();
				showException(e, sync);
				return List.of();
			}
		}
		catch (final Exception e1)
		{
			e1.printStackTrace();
			showException(e1, sync);
			return List.of();
		}
	}
	
	public static Optional<HttpResponseW> put_WithSSL(final String url, final String body, final List<NameValuePair> params)
	{
		try {
			final SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();

			try (CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLContext(sslContext)
					.build())
			{
				final HttpPut request = new HttpPut(url);
				request.addHeader("content-type", "application/json");
				request.setEntity(new StringEntity(body));
				final URI uri = new URIBuilder(request.getURI()).addParameters(params).build();
				request.setURI(uri);
				return HttpResponseW.wrap(httpClient.execute(request));
			}
			catch (final ClientProtocolException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final URISyntaxException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1)
		{
			e1.printStackTrace();
			showException(e1);
			return Optional.empty();
		}
	}
	
	public static Optional<HttpResponseW> post_WithSSL(final String url, final String body, final List<NameValuePair> params)
	{
		try {
			final SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();

			try (CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLContext(sslContext)
					.build())
			{
				final HttpPost request = new HttpPost(url);
				request.addHeader("content-type", "application/json");
				request.setEntity(new StringEntity(body));
				final URI uri = new URIBuilder(request.getURI()).addParameters(params).build();
				request.setURI(uri);
				return HttpResponseW.wrap(httpClient.execute(request));
			}
			catch (final ClientProtocolException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final URISyntaxException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1)
		{
			e1.printStackTrace();
			showException(e1);
			return Optional.empty();
		}
	}
	
	public static Optional<Long> post_WithSSL_DownloadFile(final String url, final String body,
			final String outputFileUri, final List<NameValuePair> params)
	{
		return post_WithSSL_DownloadFile(url, body, outputFileUri, params, Map.of("content-type", "application/json"));
	}
	
	public static Optional<Long> post_WithSSL_DownloadFile(final String url, final String body,
			final String outputFileUri, final List<NameValuePair> params, final Map<String, String> headers)
	{
		try {
			final SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();

			try (CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLContext(sslContext)
					.build();)
			{
				final HttpPost request = new HttpPost(url);
				headers.forEach(request::addHeader);
				request.setEntity(new StringEntity(body));
				final URI uri = new URIBuilder(request.getURI()).addParameters(params).build();
				request.setURI(uri);
				
				final CloseableHttpResponse response = httpClient.execute(request);
				final int statusCode = response.getStatusLine().getStatusCode();
				
				if (HttpStatus.SC_OK != statusCode)
					throw new UnsupportedOperationException("Response status code: "+statusCode);
				
				final Header contentHeader = response.getFirstHeader("content-type");
				if (contentHeader != null && contentHeader.getValue().equalsIgnoreCase("application/json"))
					return UIUtils.copyFileFromTo(response.getEntity().getContent(), UIUtils.removeFileExtension(outputFileUri)+"_errors.txt");
				else
					return UIUtils.copyFileFromTo(response.getEntity().getContent(), outputFileUri);
				
			}
			catch (final ClientProtocolException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
			catch (final URISyntaxException e)
			{
				e.printStackTrace();
				showException(e);
				return Optional.empty();
			}
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1)
		{
			e1.printStackTrace();
			showException(e1);
			return Optional.empty();
		}
	}
	
	public static class HttpResponseW
	{
		final private int statusCode;
		final private String reasonPhrase;
		final private String entity;
		
		public static Optional<HttpResponseW> wrap(final HttpResponse resp) throws UnsupportedOperationException, IOException
		{
			if (resp == null)
				return Optional.empty();
			
			final String entity = IOUtils.toString(resp.getEntity().getContent(), StandardCharsets.UTF_8.toString());
			final int statusCode = resp.getStatusLine().getStatusCode();
			final String reasonPhrase = readJsonProperty(entity, "message")
					.orElse(resp.getStatusLine().getReasonPhrase());
			
			return Optional.of(new HttpResponseW(statusCode, reasonPhrase, entity));
		}
		
		private HttpResponseW(final int statusCode, final String reasonPhrase, final String entity)
		{
			super();
			this.statusCode = statusCode;
			this.reasonPhrase = reasonPhrase;
			this.entity = entity;
		}

		public int getStatusCode()
		{
			return statusCode;
		}
		
		public String getReasonPhrase()
		{
			return reasonPhrase;
		}
		
		public String getEntity()
		{
			return entity;
		}
	}
}
