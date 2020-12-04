package com.demo.connectrn;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.demo.connectrn.api.ReservationApi;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * JAX-RS Application configuration class.
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(
		title = "ConnectRN Restaurant",
		version = "1",
		description = "Quick Restaurant Demo",
		license = @License(name = "MIT"))
)
public class RestaurantApplication extends ResourceConfig {

	public static final int DEFAULT_LISTEN_PORT = 8080;

	public RestaurantApplication() {
		register(JacksonJsonProvider.class);
		register(LoggingFilter.class);
		register(new AbstractBinder() {
			protected @Override void configure() {
				addActiveDescriptor(RestaurantDao.class);
			};
		});
		packages(ReservationApi.class.getPackage().getName());
	}

	/** Use grizzly to run locally for testing. */
	public static void main(String[] args) throws IOException {
		URI baseUri = UriBuilder.fromUri("http://localhost/api").port(DEFAULT_LISTEN_PORT).build();
		ResourceConfig config = new RestaurantApplication();
		HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
		
		// Add the swagger HTML as static content
		HttpHandler apiDocsHandler = new StaticHttpHandler("src/main/webapp");
		httpServer.getServerConfiguration().addHttpHandler(apiDocsHandler, "/");

		httpServer.start();
	}
}
