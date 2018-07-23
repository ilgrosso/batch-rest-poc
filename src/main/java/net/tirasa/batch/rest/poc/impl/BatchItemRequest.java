package net.tirasa.batch.rest.poc.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import net.tirasa.batch.rest.poc.batch.BatchItem;
import org.springframework.http.MediaType;

public class BatchItemRequest extends HttpServletRequestWrapper {

    private final BatchItem batchItem;

    private final ServletInputStream inputStream;

    public BatchItemRequest(
            final HttpServletRequest request,
            final BatchItem batchItem) {

        super(request);
        this.batchItem = batchItem;
        this.inputStream = new ServletInputStream() {

            private final ByteArrayInputStream bais = new ByteArrayInputStream(batchItem.getContent().getBytes());

            private boolean isFinished = false;

            private boolean isReady = true;

            @Override
            public boolean isFinished() {
                return isFinished;
            }

            @Override
            public boolean isReady() {
                return isReady;
            }

            @Override
            public void setReadListener(final ReadListener readListener) {
                // nope
            }

            @Override
            public int read() {
                isFinished = true;
                isReady = false;
                return bais.read();
            }
        };
    }

    @Override
    public String getMethod() {
        return batchItem.getMethod();
    }

    @Override
    public String getRequestURI() {
        return batchItem.getRequestURI();
    }

    @Override
    public String getQueryString() {
        return batchItem.getQueryString();
    }

    @Override
    public String getContentType() {
        return batchItem.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)
                ? batchItem.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0).toString()
                : MediaType.ALL_VALUE;
    }

    @Override
    public int getContentLength() {
        return batchItem.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)
                ? Integer.valueOf(batchItem.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0).toString())
                : 0;
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    @Override
    public String getHeader(final String name) {
        return batchItem.getHeaders().containsKey(name)
                ? batchItem.getHeaders().get(name).get(0).toString()
                : HttpHeaders.CONTENT_TYPE.equals(name)
                ? MediaType.ALL_VALUE
                : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return batchItem.getHeaders().containsKey(name)
                ? Collections.enumeration(
                        batchItem.getHeaders().get(name).stream().map(Object::toString).collect(Collectors.toList()))
                : HttpHeaders.CONTENT_TYPE.equals(name)
                ? Collections.enumeration(Arrays.asList(MediaType.ALL_VALUE))
                : super.getHeaders(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }
}
