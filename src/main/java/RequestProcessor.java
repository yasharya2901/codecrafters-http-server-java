import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RequestProcessor implements Runnable {
    private String path;
    private String httpRequestHeader;
    private String httpRequestBody;
    private String requestMethod;
    private Socket clientRequest;
    private OutputStream outputStream;
    private Map<String, String> headers = new HashMap<>();

    public RequestProcessor(String path, String httpRequestMethod, String httpRequestHeader, String httpRequestBody, Socket clientRequest) {
        this.path = path;
        this.httpRequestHeader = httpRequestHeader;
        this.httpRequestBody = httpRequestBody;
        this.requestMethod = httpRequestMethod;
        this.clientRequest = clientRequest;
        try {
            this.outputStream = clientRequest.getOutputStream();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
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



    private CompressedReturnObject getHttpResponse(int responseCode, String responseMessage, String message, String contentType, String compressionType) throws IOException {
        String[] compressionTypes = compressionType.split(",");
        String compressionUsed = "";
        byte [] gzipData = null;
        for (String type: compressionTypes) {
            switch (type.trim()) {
                case "gzip":
                    compressionUsed = SupportedCompression.gzip.toString();
                    break;
                case "deflate":
                    compressionUsed = SupportedCompression.deflate.toString();
                    break;
                case "br":
                    compressionUsed = SupportedCompression.br.toString();
                    break;
            }
        }
        if (compressionUsed.equals("")) {
            return new CompressedReturnObject(getHttpResponse(responseCode, responseMessage, message, contentType), null);
        }

        if (compressionUsed.equals(SupportedCompression.gzip.toString())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                gzipOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
                gzipOutputStream.close();
            }
            catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
            gzipData = byteArrayOutputStream.toByteArray();


        }

        return new CompressedReturnObject("HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n" +
                "Content-Type: "+contentType+"\r\n" +
                "Content-Length: " + ((gzipData != null) ? gzipData.length : 0) + "\r\n" +
                "Content-Encoding: "+ compressionUsed +"\r\n" +
                "\r\n", gzipData);
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
                    httpResponse = getHttpResponse(200, "OK", message, "text/plain");
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    clientRequest.getOutputStream().write(httpResponse.getBytes());
                } else if (pathParts.length == 3) {
                    String message = pathParts[2];
                    String httpResponse;
                    CompressedReturnObject compressedReturnObject = null;
                    if (this.headers.containsKey("accept-encoding") && this.headers.get("accept-encoding").contains("gzip")) {
                        compressedReturnObject = getHttpResponse(200, "OK", message, "text/plain", this.headers.get("accept-encoding"));
                        httpResponse = compressedReturnObject.getMessage();
                    } else {
                        httpResponse = getHttpResponse(200, "OK", message, "text/plain");
                    }
                    System.out.println("Sending response: " + httpResponse);
                    System.out.println(endLine);
                    if (compressedReturnObject != null && compressedReturnObject.getData() != null) {
                        outputStream.write(httpResponse.getBytes());
                        outputStream.write(compressedReturnObject.getData());
                        clientRequest.close();
                        return;
                    }
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
