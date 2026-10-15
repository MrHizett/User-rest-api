package userapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RequestHandler implements HttpHandler {
    private Map<Long, User> users = new HashMap<>();
    private long nextId = 1;
    private final ObjectMapper om = new ObjectMapper();
    private final File file = new File("users.json");


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try{
            String method = exchange.getRequestMethod();
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
                        save();
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
                        save();
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
                            save();
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

    private void save() throws IOException {
        om.writeValue(new File("users.json"), users.values());
    }

    public RequestHandler() throws IOException {
        if(file.exists() && file.length() > 0){
            List<User> loaded = om.readValue(file, new TypeReference<List<User>>() {});
            for(User u : loaded){
                users.put(u.getId(), u);
            }
            long max = 0;
            for (User u : users.values()){
                max = Math.max(max, u.getId());
            }
            nextId = max + 1;
        }
    }
}