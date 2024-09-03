import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    try {
      ServerSocket serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientRequest = serverSocket.accept();
        System.out.println("accepted new connection");

        InputStream input = clientRequest.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String httpRequest = reader.readLine();
        if (httpRequest == null) {
          continue;
        }

        StringBuilder httpRequestHeader = new StringBuilder();
        String line;

        // Read the headers
        while (!(line = reader.readLine()).isEmpty()) {
          httpRequestHeader.append(line).append("\n");
        }

        // Read the body
        StringBuilder httpRequestBody = new StringBuilder();
        while (reader.ready() && (line = reader.readLine()) != null) {
          httpRequestBody.append(line).append("\n");
        }

        System.out.println("Received request:\n" + httpRequest + "\n");
        System.out.println("Received request header:\n" + httpRequestHeader + "\n");
        System.out.println("Received request body:\n" + httpRequestBody + "\n");

        String[] requestParts = httpRequest.split(" ");
        String path = requestParts[1];
        System.out.println("Path: " + path);
        processRequest(path, httpRequestHeader.toString(), httpRequestBody.toString(), clientRequest);
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
  }

  private static String getHttpResponse(int responseCode, String responseMessage, String message) {
    return "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
           "Content-Type: text/plain\r\n" +
           "Content-Length: " + message.length() + "\r\n" +
           "\r\n" +
           message;
  }

    private static void processRequest(String path, String httpRequestHeader, String httpRequestBody, Socket clientRequest) throws IOException {
        String endLine = "-----------------------------------------------------------------";
        String[] pathParts = path.split("/");
        for (int i = 0; i < pathParts.length; i++) {
            System.out.println("Path part:[" + i + "]: " + pathParts[i]);
        }
        String httpResponse;
        if (pathParts.length == 0) {
            String message = "Welcome to the Homepage!";
            httpResponse = getHttpResponse(200, "OK", message);
        } else if (pathParts.length >= 2 && pathParts[1].equals("echo")) {
            if (pathParts.length == 2) {
                String message = "Please provide a message to echo";
                httpResponse = getHttpResponse(200, "OK", message);
            } else {
                String message = pathParts[2];
                httpResponse = getHttpResponse(200, "OK", message);
            }
        } else if (pathParts[1].equals("user-agent")) {
            // Properly normalize header line endings to \r\n
            String normalizedHeader = httpRequestHeader.replaceAll("\n", "\r\n");
            httpResponse = getHttpResponse(200, "OK", normalizedHeader);
        } else {
            String message = "The requested path is not available";
            httpResponse = getHttpResponse(404, "Not Found", message);
        }

        System.out.println("Sending response: " + httpResponse);
        System.out.println(endLine);
        clientRequest.getOutputStream().write(httpResponse.getBytes());
        clientRequest.close();
    }

}