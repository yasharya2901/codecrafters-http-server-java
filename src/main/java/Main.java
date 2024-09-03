import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args) {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    System.out.println("Logs from your program will appear here!");

    try {
      ServerSocket serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientRequest = serverSocket.accept();
        System.out.println("accepted new connection");

        executorService.execute(() ->{
          try {
            InputStream input = clientRequest.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String httpRequest = reader.readLine();
            if (httpRequest == null) {
              clientRequest.close();
              return;
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
            RequestProcessor requestProcessor = new RequestProcessor(path, httpRequestHeader.toString(), httpRequestBody.toString(), clientRequest);
            requestProcessor.processRequest();
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
          }
        });
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
  }
}