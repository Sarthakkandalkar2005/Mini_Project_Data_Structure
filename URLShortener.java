import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Random;

public class URLShortener {


    private static CustomHashMap urlMap = new CustomHashMap();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/shorten", new ShortenHandler());
        server.createContext("/go", new RedirectHandler());

        server.setExecutor(null);
        System.out.println("Server started at http://localhost:8080");
        server.start();
    }

    static class ShortenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("=")) {
                String response = "Missing url parameter";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            String urlParam = query.split("=")[1];
            String decodedUrl = URLDecoder.decode(urlParam, "UTF-8");

            String shortCode = generateCode(6);
            urlMap.put(shortCode, decodedUrl);

            String response = shortCode;
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            System.out.println("Shortened: " + decodedUrl + " -> " + shortCode);
        }
    }

    static class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String path = exchange.getRequestURI().getPath();
            String prefix = "/go/";
            if (!path.startsWith(prefix)) {
                String response = "Invalid request";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            String code = path.substring(prefix.length());

            String originalUrl = urlMap.get(code);

            if (originalUrl == null) {
                String response = "URL not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            exchange.getResponseHeaders().add("Location", originalUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();

            System.out.println("Redirecting code " + code + " to " + originalUrl);
        }
    }

    private static String generateCode(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    static class CustomHashMap {
        private static final int SIZE = 100;
        private Entry[] buckets;

        public CustomHashMap() {
            buckets = new Entry[SIZE];
        }

        public void put(String key, String value) {
            int index = hash(key);
            Entry head = buckets[index];

            for (Entry curr = head; curr != null; curr = curr.next) {
                if (curr.key.equals(key)) {
                    curr.value = value;
                    return;
                }
            }

            Entry newEntry = new Entry(key, value);
            newEntry.next = head;
            buckets[index] = newEntry;
        }

        public String get(String key) {
            int index = hash(key);
            Entry head = buckets[index];

            for (Entry curr = head; curr != null; curr = curr.next) {
                if (curr.key.equals(key)) {
                    return curr.value;
                }
            }
            return null;
        }

        private int hash(String key) {
            int hashCode = 0;
            for (int i = 0; i < key.length(); i++) {
                hashCode = (31 * hashCode + key.charAt(i)) % SIZE;
            }
            return hashCode;
        }

        static class Entry {
            String key;
            String value;
            Entry next;

            Entry(String key, String value) {
                this.key = key;
                this.value = value;
                this.next = null;
            }
        }
    }
}
