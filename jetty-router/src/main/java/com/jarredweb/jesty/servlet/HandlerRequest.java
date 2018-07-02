package com.jarredweb.jesty.servlet;

import com.google.gson.Gson;
import com.jarredweb.jesty.route.AppRoute;
import com.jarredweb.jesty.route.RouteRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jarredweb.jesty.route.BodyReader;

public class HandlerRequest extends HttpServletRequestWrapper implements RouteRequest {

    private final Logger LOG = LoggerFactory.getLogger(HandlerRequest.class);
    protected AppRoute route;
    protected boolean error;
    protected String message;
    protected byte[] body;
    protected Cookie[] cookies;

    public HandlerRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String protocol() {
        return getProtocol();
    }

    @Override
    public boolean secure() {
        return protocol().toLowerCase().equals("https");
    }

    @Override
    public String hostname() {
        return getRemoteHost();
    }

    @Override
    public String ip() {
        return getRemoteAddr();
    }

    @Override
    public AppRoute route() {
        return this.route;
    }

    @Override
    public void route(AppRoute route) {
        this.route = route;
    }

    @Override
    public String path() {
        return getRequestURI();
    }

    @Override
    public String param(String name) {
        String value = getParameter(name);
        return (value != null) ? value : pathParams().get(name);
    }

    @Override
    public String query() {
        return getQueryString();
    }

    @Override
    public String header(String name) {
        return getHeader(name);
    }

    @Override
    public boolean error() {
        return this.error;
    }

    @Override
    public String message() {
        return this.message;
    }

    @Override
    public byte[] body() {
        return this.body;
    }

    @Override
    public <T> T body(Class<T> type) {
        String contentType = header("ContentType");
        if (contentType.contains("application/json")) {
            Reader reader = new InputStreamReader(new ByteArrayInputStream(body()));
            Gson gson = new Gson();
            return gson.fromJson(reader, type);
        }
        if (contentType.contains("application/xml")) {
            try {
                Reader reader = new InputStreamReader(new ByteArrayInputStream(body()));
                JAXBContext context = JAXBContext.newInstance(type);
                Unmarshaller un = context.createUnmarshaller();
                return (T) un.unmarshal(reader);
            } catch (JAXBException e) {
                LOG.error(e.getMessage());
            }
        }
        throw new RuntimeException("Failed to tranform the request body. Try using a BodyProvider<T> instead");
    }

    @Override
    public <T> T body(BodyReader<T> provider) {
        String type = header("ContentType");
        return provider.transform(type, body());
    }

    @Override
    public Cookie[] cookies() {
        return this.cookies;
    }

    @Override
    public HttpSession session(boolean create) {
        return getSession(create);
    }

    @Override
    public long upload(String dest) {
        String homeDir = System.getProperty("user.dir");
        // constructs path of the directory to save uploaded file
        String savePath = "/www/media/".concat((dest != null && dest.trim().length() > 0) ? dest : "upload");

        // creates the save directory if it does not exists
        File fileSaveDir = new File(homeDir, savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        long size = 0;
        try {
            for (Part part : getParts()) {
                String fileName = extractFileName(part);
                if (fileName != null && fileName.trim().length() > 0) {
                    // refines the fileName in case it is an absolute path
                    fileName = new File(fileName).getName();
                    part.write(fileSaveDir.getPath() + File.separator + fileName);
                    size += part.getSize();
                }
            }
        } catch (IOException | ServletException e) {
            this.error = true;
            this.message = e.getMessage();
            return -1;
        }
        this.message = "Upload has been done successfully!";
        return size;
    }

    @Override
    public long capture() {
        //content queue
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ReadableByteChannel inChannel = Channels.newChannel(getInputStream())) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            int bytesRead = inChannel.read(buffer); //read into buffer.
            while (bytesRead != -1) {
                buffer.flip();  //make buffer ready for read

                if (buffer.hasRemaining()) {
                    byte[] xfer = new byte[buffer.limit()]; //transfer buffer bytes to a different aray
                    buffer.get(xfer);
                    bytes.write(xfer); // read entire array backing buffer
                }

                buffer.clear(); //make buffer ready for writing
                bytesRead = inChannel.read(buffer);
            }
            LOG.debug("Completed reading content from input channel. bytes size = {} * {}", bytes.size());
            body = bytes.toByteArray();
        } catch (IOException e) {
            this.error = true;
            this.message = e.getMessage();
            return -1;
        }
        //return approx size
        return bytes.size();
    }

    /**
     * Extracts file name from HTTP header content-disposition
     */
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }

    @Override
    public Map<String, String> pathParams() {
        return route.pathParams;
    }
}