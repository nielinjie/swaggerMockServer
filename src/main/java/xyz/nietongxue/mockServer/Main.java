package xyz.nietongxue.mockServer;

/**
 * Created by nielinjie on 9/10/16.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        MockServer server = new MockServer(args[0]);
        server.start();
    }
}
