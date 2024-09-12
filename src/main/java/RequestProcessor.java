import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RequestProcessor implements Runnable {
    private String path;
    private String httpRequestHeader;
    private String httpRequestBody;
    private String requestMethod;
    private Socket clientRequest;
    private Map<String, String> headers = new HashMap<>();

    public RequestProcessor(String path, String httpRequestMethod, String httpRequestHeader, String httpRequestBody, Socket clientRequest) {
        this.path = path;
        this.httpRequestHeader = httpRequestHeader;
        this.httpRequestBody = httpRequestBody;
        this.requestMethod = httpRequestMethod;
        this.clientRequest = clientRequest;
        String[] parts = httpRequestHeader.split("\n");
        for (String part: parts) {
            String[] attr = part.split(":");
            if (attr.length == 2) {
                this.headers.put(attr[0].toLowerCase().trim(), attr[1].trim());
            }
        }

    }

    private String getHttpResponse(int responseCode, String responseMessage, String message, String contentType) {
        return "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
                "Content-Type: "+contentType+"\r\n" +
                "Content-Length: " + message.getBytes().length + "\r\n" +
                "\r\n" +
                message;
    }

    private String getHttpResponse(int responseCode, String responseMessage, String message, String contentType, String compressionType) {
        return "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
                "Content-Type: "+contentType+"\r\n" +
                "Content-Length: " + message.getBytes().length + "\r\n" +
                "Content-Encoding: gzip\r\n" +
                "\r\n" +
                message;
    }

    private String getHttpResponse(int responseCode, String responseMessage) {
        return "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
                "\r\n";
    }

    public void processRequest() throws IOException, InterruptedException {
        String endLine = "-----------------------------------------------------------------";
        String[] pathParts = path.split("/");
        for (int i = 0; i < pathParts.length; i++) {
            System.out.println("Path part:[" + i + "]: " + pathParts[i]);
        }

        if (this.requestMethod.equals("POST")) {
            if (pathParts.length == 3) {
                if (pathParts[1].equals("files")) {
                    String fileName = pathParts[2];
                    String fullPath = Main.directoryPath + File.separator + fileName;
                    String fileContent = this.httpRequestBody;
                    Path filePath = Paths.get(fullPath);
                    if (!Files.exists(filePath)) {
                        Files.createFile(filePath);
                    }
                    Files.writeString(filePath, fileContent);
                    String httpResponse = getHttpResponse(201, "Created");
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                }
            } else if (pathParts.length == 2) {
                if (pathParts[1].equals("files")) {
                    String message = "Please provide a file name";
                    String httpResponse = getHttpResponse(404, "Not Found", message, "text/plain");
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                    clientRequest.close();
                    return;
                }
            }

            clientRequest.close();
            return;
        }

        if (pathParts.length == 0) {
            String message = "";
            String httpResponse = getHttpResponse(200, "OK", message, "text/plain");
            System.out.println("Sending response: " + httpResponse);
            System.out.println(endLine);
            clientRequest.getOutputStream().write(httpResponse.getBytes());
        } else if (pathParts.length != 0) {
            if (pathParts.length >= 2 && pathParts[1].equals("echo")) {
                if (pathParts.length == 2) {
                    String httpResponse;
                    String message = "Please provide a message to echo";
                    if (this.headers.containsKey("accept-encoding") && this.headers.get("accept-encoding").contains("gzip")) {
                        httpResponse = getHttpResponse(200, "OK", message, "text/plain", "gzip");
                    } else {
                        httpResponse = getHttpResponse(200, "OK", message, "text/plain");
                    }
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                } else if (pathParts.length == 3) {
                    String message = pathParts[2];
                    String httpResponse;
                    if (this.headers.containsKey("accept-encoding") && this.headers.get("accept-encoding").contains("gzip")) {
                        httpResponse = getHttpResponse(200, "OK", message, "text/plain", "gzip");
                    } else {
                        httpResponse = getHttpResponse(200, "OK", message, "text/plain");
                    }
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                }
            } else if (pathParts[1].equals("user-agent")) {
                String message = headers.get("user-agent");
                String httpResponse = getHttpResponse(200, "OK", message, "text/plain");
                System.out.println("Sending response: " + httpResponse);
                System.out.println(endLine);
                clientRequest.getOutputStream().write(httpResponse.getBytes());
            } else if (pathParts.length >=2 && pathParts[1].equals("files")) {
                  if (pathParts.length == 2) {
                      String message = "Please provide a file name";
                      String httpResponse = getHttpResponse(404, "Not Found", message, "text/plain");
                      System.out.println("Sending response: " + httpResponse);
                      System.out.println(endLine);
                      clientRequest.getOutputStream().write(httpResponse.getBytes());
                      clientRequest.close();
                      return;
                  }

                  String fileName = pathParts[2];
                  String fullPath = Main.directoryPath + (Main.OPERATING_SYSTEM.split(" ")[0].toLowerCase().equals("windows") ? "\\" : "/") + fileName;
                  Path filePath = Paths.get(fullPath);
                  if (!Files.exists(filePath)) {
                        String message = "The requested file is not available";
                        String httpResponse = getHttpResponse(404, "Not Found", message, "text/plain");
                        System.out.println("Sending response: " + httpResponse);
                        System.out.println(endLine);
                        clientRequest.getOutputStream().write(httpResponse.getBytes());
                        clientRequest.close();
                        return;
                  }
                  
                  String message = Files.readString(filePath);
                  String httpResponse = getHttpResponse(200, "OK", message, "application/octet-stream");
                  System.out.println("Sending response: " + httpResponse);
                  System.out.println(endLine);
                  clientRequest.getOutputStream().write(httpResponse.getBytes());
            } else {
                String message = "The requested path is not available";
                String httpResponse = getHttpResponse(404, "Not Found", message, "text/plain");
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
