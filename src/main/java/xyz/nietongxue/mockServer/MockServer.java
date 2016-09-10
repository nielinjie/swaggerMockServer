package xyz.nietongxue.mockServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Created by nielinjie on 9/10/16.
 */
public class MockServer {

    private final Server server;

    public MockServer() {
        server = new Server(8080);
        ServletContextHandler handler =new ServletContextHandler();
        handler.setContextPath("/");
        ServletHolder holder =  handler.addServlet(org.glassfish.jersey.servlet.ServletContainer.class,"/*");
        holder.setInitParameter("javax.ws.rs.Application","io.swagger.inflector.SwaggerInflector");
        holder.setInitOrder(1);
        server.setHandler(handler);

    }

    public void start() throws Exception {
        server.start();
    }
    public void stop() throws Exception {
        server.stop();
    }
}
