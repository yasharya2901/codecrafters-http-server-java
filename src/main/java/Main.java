import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class Main {
      static String directoryPath;
      static String OPERATING_SYSTEM = System.getProperty("os.name");
      public static void main(String[] args) {
          System.out.println("Current Operating System: " + OPERATING_SYSTEM);
          if (args.length != 0) {
              if (args.length % 2 != 0) {
                  System.out.println("Invalid number of arguments");
                  return;
              }
              if (args[0].equals("--directory")) {
                  directoryPath = args[1];
                  if (!doesDirectoryExist(directoryPath)) {
                      System.out.println("Directory does not exist");
                      return;
                  }
                  System.out.println("Directory exists");
              }
          }
          try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
              System.out.println("Logs from your program will appear here!");



              try {
                  ServerSocket serverSocket = new ServerSocket(4221);
                  serverSocket.setReuseAddress(true);

                  while (true) {
                      Socket clientRequest = serverSocket.accept();
                      System.out.println("accepted new connection");

                      executorService.execute(() -> {
                          try {
                              InputStream input = clientRequest.getInputStream();
                              BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                              String httpRequest = reader.readLine();
                              if (httpRequest == null) {
                                  clientRequest.close();
                                  return;
                              }

                              StringBuffer httpRequestHeader = new StringBuffer();
                              String line;

                              // Read the headers
                              while (!(line = reader.readLine()).isEmpty()) {
                                  httpRequestHeader.append(line).append("\n");
                              }

                              // Read the body
                              StringBuffer bodyBuffer = new StringBuffer();
                              while (reader.ready()) {
                                  bodyBuffer.append((char)reader.read());
                              }
                              String httpRequestBody = bodyBuffer.toString();


                              System.out.println("Received request:\n" + httpRequest + "\n");
                              System.out.println("Received request header:\n" + httpRequestHeader + "\n");
                              System.out.println("Received request body:\n" + httpRequestBody + "\n");

                              String[] requestParts = httpRequest.split(" ");
                              String requestMethod = requestParts[0];
                              String path = requestParts[1];
                              System.out.println("Path: " + path);
                              RequestProcessor requestProcessor = new RequestProcessor(path, requestMethod, httpRequestHeader.toString(), httpRequestBody.toString(), clientRequest);
                              requestProcessor.processRequest();
                          } catch (IOException | InterruptedException e) {
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
          } catch (Exception e) {
              System.out.println("Exception: " + e.getMessage());
          } finally {
                System.out.println("Shutting down");
          }
      }

      private static boolean doesDirectoryExist(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
      }
}