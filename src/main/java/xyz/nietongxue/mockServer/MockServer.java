package xyz.nietongxue.mockServer;

import io.swagger.inflector.SwaggerInflector;
import io.swagger.inflector.config.Configuration;
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

    public MockServer(String swaggerUrl) {
        Configuration configuration = Configuration.defaultConfiguration().swaggerUrl(swaggerUrl);

        URI baseUri = UriBuilder.fromUri("http://localhost/").port(8080).build();
        ResourceConfig config = new SwaggerInflector(configuration);
        server = JettyHttpContainerFactory.createServer(baseUri, config);

    }

    public void start() throws Exception {
        server.start();
    }
    public void stop() throws Exception {
        server.stop();
    }
}
