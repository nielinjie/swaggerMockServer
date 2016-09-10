package xyz.nietongxue.mockServer;

/**
 * Created by nielinjie on 9/10/16.
 */
public class Main {
    public static void main(String[] args) throws Exception {
//        final StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
//        System.out.println(binder.getLoggerFactory());
//        System.out.println(binder.getLoggerFactoryClassStr());
        MockServer mockServer=new MockServer(args[0]);
        mockServer.start();

    }
}
