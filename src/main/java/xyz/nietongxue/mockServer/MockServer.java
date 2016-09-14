package xyz.nietongxue.mockServer;

import io.swagger.inflector.SwaggerInflector;
import io.swagger.inflector.config.Configuration;
import io.swagger.models.Swagger;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Created by nielinjie on 9/10/16.
 */
public class MockServer {

    private final Server server;
    private Swagger swagger;

    public MockServer(String swaggerUrl) {
        this(swaggerUrl, 8080);
    }

    public MockServer(String swaggerUrl, int port) {
        Configuration configuration = Configuration.defaultConfiguration().swaggerUrl(swaggerUrl);
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
        SwaggerInflector inflector = new SwaggerInflector(configuration);
        server = JettyHttpContainerFactory.createServer(baseUri, inflector);
        swagger = inflector.getSwagger();
    }

    public Swagger getSwagger() {
        return swagger;
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
