import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    //
     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);
        while(true) {
           Socket clientRequest = serverSocket.accept(); // Wait for connection from client.
           System.out.println("accepted new connection");
             InputStream input = clientRequest.getInputStream();


             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             String httpRequest = reader.readLine();
             // httpRequest is something like "GET / HTTP/1.1" or "GET /index.html HTTP/1.1"
             // The first part is the request method.
             // The second part is the path.
             // The third part is the HTTP version.

             System.out.println("Received request: " + httpRequest);

             if (httpRequest == null) {
                 continue;
             }

             String[] requestParts = httpRequest.split(" ");
             String path = requestParts[1];
             System.out.println("Path: "+path);
             processRequest(path, clientRequest);
        }

     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     } catch (Exception e) {
         System.out.println("Exception: " + e.getMessage());
     }
  }

  private static String getHttpResponse(int responseCode, String responseMessage, String message) {
        return "HTTP/1.1 "+responseCode+" "+responseMessage+"\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: "+ message.length()+"\r\n" +
                "\r\n" +
                message;
  }

  private static void processRequest(String path, Socket clientRequest) throws IOException {
      String endLine = "-----------------------------------------------------------------";
      String[] pathParts = path.split("/");
      for (int i = 0; i < pathParts.length; i++) {
            System.out.println("Path part:["+i+"]: " + pathParts[i]);
      }
      if (pathParts.length == 0) {
          // The path is "/"
          String message = "Welcome to the Homepage!";
          String httpResponse = getHttpResponse(200, "OK", message);
          System.out.println("Sending response: " + httpResponse);
          System.out.println(endLine);
          clientRequest.getOutputStream().write(httpResponse.getBytes());
      }

      else if (pathParts[1].equals("echo")) {
          // The path is "/echo"
          if (pathParts.length == 2) {
              String message = "Please provide a message to echo";
              String httpResponse = getHttpResponse(200, "OK", message);
              System.out.println("Sending response: " + httpResponse);
              System.out.println(endLine);
              clientRequest.getOutputStream().write(httpResponse.getBytes());
          }

          else if (pathParts.length == 3) {
              String message = pathParts[2];
              String httpResponse = getHttpResponse(200, "OK", message);
              System.out.println("Sending response: " + httpResponse);
              System.out.println(endLine);
              clientRequest.getOutputStream().write(httpResponse.getBytes());
          }
      } else {
          // The path is not recognized
          String message = "The requested path is not available";
            String httpResponse = getHttpResponse(404, "Not Found", message);
            System.out.println("Sending response: " + httpResponse);
          System.out.println(endLine);
            clientRequest.getOutputStream().write(httpResponse.getBytes());
      }
  }
}
