import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class RequestProcessor implements Runnable {
    private String path;
    private String httpRequestHeader;
    private String httpRequestBody;
    private Socket clientRequest;

    public RequestProcessor(String path, String httpRequestHeader, String httpRequestBody, Socket clientRequest) {
        this.path = path;
        this.httpRequestHeader = httpRequestHeader;
        this.httpRequestBody = httpRequestBody;
        this.clientRequest = clientRequest;
    }

    private String getHttpResponse(int responseCode, String responseMessage, String message) {
        return "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + message.getBytes().length + "\r\n" +
                "\r\n" +
                message;
    }

    public void processRequest() throws IOException, InterruptedException {
        String endLine = "-----------------------------------------------------------------";
        String[] pathParts = path.split("/");
        for (int i = 0; i < pathParts.length; i++) {
            System.out.println("Path part:[" + i + "]: " + pathParts[i]);
        }
        if (pathParts.length == 0) {
            String message = "";
            String httpResponse = getHttpResponse(200, "OK", message);
            System.out.println("Sending response: " + httpResponse);
            System.out.println(endLine);
            clientRequest.getOutputStream().write(httpResponse.getBytes());
        } else if (pathParts.length != 0) {
            if (pathParts.length >= 2 && pathParts[1].equals("echo")) {
                if (pathParts.length == 2) {
                    String message = "Please provide a message to echo";
                    String httpResponse = getHttpResponse(200, "OK", message);
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                } else if (pathParts.length == 3) {
                    String message = pathParts[2];
                    String httpResponse = getHttpResponse(200, "OK", message);
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                }
            } else if (pathParts[1].equals("user-agent")) {
                String[] parts = httpRequestHeader.split("[\\s\\n]+");
                Map<String, String> headers = new HashMap<>();
                for (int i = 0; i < parts.length; i += 2) {
                    if (parts[i].charAt(parts[i].length() - 1) == ':') {
                        parts[i] = parts[i].substring(0, parts[i].length() - 1);
                    }
                    headers.put(parts[i].toLowerCase(), parts[i + 1]);
                }
                String message = headers.get("user-agent");
                String httpResponse = getHttpResponse(200, "OK", message);
                System.out.println("Sending response: " + httpResponse);
                System.out.println(endLine);
                clientRequest.getOutputStream().write(httpResponse.getBytes());
            } else {
                String message = "The requested path is not available";
                String httpResponse = getHttpResponse(404, "Not Found", message);
                System.out.println("Sending response: " + httpResponse);
                System.out.println(endLine);
                clientRequest.getOutputStream().write(httpResponse.getBytes());
            }
        }
        clientRequest.close();
    }

    @Override
    public void run() {
        try {
            processRequest();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
