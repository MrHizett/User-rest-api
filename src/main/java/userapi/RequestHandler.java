package userapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class RequestHandler implements HttpHandler {
    private Map<Long, User> users = new HashMap<>();
    private long nextId = 4;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try{
            String method = exchange.getRequestMethod();
            ObjectMapper om = new ObjectMapper();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = 200;
            String json = null;

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            boolean hasId = parts.length == 3 && !parts[2].isBlank();

            if("GET".equals(method) && !hasId){
                json = om.writeValueAsString(users.values());
            }
            else if("GET".equals(method) && hasId){
                try{
                    long id = Long.parseLong(parts[2]);
                    User found = users.get(id);
                    if(found == null){
                        status = 404;
                    }
                    else {
                        json = om.writeValueAsString(found);
                    }
                }
                catch (NumberFormatException e){
                    status = 404;
                }
            } else if("POST".equals(method)){
                if(body == null || body.isBlank()){
                    status = 400;
                }else {
                    try {
                        User incoming = om.readValue(body, User.class);
                        User storedUser = new User(nextId, incoming.getName(), incoming.getEmail());
                        users.put(nextId, storedUser);
                        nextId++;
                        json = om.writeValueAsString(storedUser);
                        status = 201;
                    }catch (JsonProcessingException e){
                        status = 400;
                    }
                }
            }else if("DELETE".equals(method) && hasId){
                try{
                    long id = Long.parseLong(parts[2]);
                    User found = users.get(id);
                    if(found == null){
                        status = 404;
                    }
                    else{
                        users.remove(id);
                        status = 204;
                    }
                }
                catch (NumberFormatException e){
                    status = 404;
                }
            }
            else if("PUT".equals(method) && hasId){
                try {
                    long id = Long.parseLong(parts[2]);
                    User found = users.get(id);
                    if(found == null){
                        status = 404;
                    } else if(body == null || body.isBlank()){
                        status = 400;
                    }
                    else{
                        try{
                            User incoming = om.readValue(body, User.class);
                            User storedUser = new User(id, incoming.getName(), incoming.getEmail());
                            users.replace(id, storedUser);
                            json = om.writeValueAsString(storedUser);
                        }
                        catch (JsonProcessingException e){
                            status = 400;
                        }
                    }
                }
                catch (NumberFormatException e){
                    status = 404;
                }
            }
            else{
                status = 405;
            }

            if(json == null){
                exchange.sendResponseHeaders(status, -1);
            }else{
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(bytes);
                }
            }

        }catch (IOException e){
            exchange.sendResponseHeaders(500, -1);
        }
        finally {
            exchange.close();
        }
    }

    public RequestHandler() {
        users.put(1L, new User(1L, "Aleksa", "aleksa@gmail.com"));
        users.put(2L, new User(2L, "Marko", "marko@gmail.com"));
        users.put(3L, new User(3L, "Petar", "petar@gmail.com"));
    }
}