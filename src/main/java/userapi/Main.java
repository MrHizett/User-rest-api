package userapi;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException{
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        HttpContext context = server.createContext("/users/");
        context.setHandler(new RequestHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server at: http://localhost:8080/users/");
    }

}