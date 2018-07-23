package net.tirasa.batch.rest.poc.batch;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class BatchPayloadGenerator {

    private static final String HTTP_1_1 = "HTTP/1.1";

    public static String generate(final List<BatchItem> items, final String boundary) {
        StringBuilder payload = new StringBuilder();

        items.forEach(item -> {
            payload.append(boundary).append(BatchConstants.CRLF);
            payload.append(HttpHeaders.CONTENT_TYPE).append(": ").append("application/http").append('\n');
            payload.append("Content-Transfer-Encoding: binary").append('\n');
            payload.append(BatchConstants.CRLF);

            if (item.getMethod() != null) {
                payload.append(item.getMethod()).append(' ').append(item.getRequestURI());
                if (item.getQueryString() != null) {
                    payload.append('?').append(item.getQueryString());
                }
                payload.append(' ').append(HTTP_1_1).append('\n');
            }

            if (item.getStatus() > 0) {
                payload.append(HTTP_1_1).append(' ').
                        append(item.getStatus()).append(' ').
                        append(Response.Status.fromStatusCode(item.getStatus()).getReasonPhrase()).
                        append('\n');
            }

            if (item.getHeaders() != null && !item.getHeaders().isEmpty()) {
                item.getHeaders().forEach((key, values) -> {
                    values.forEach(value -> {
                        payload.append(key).append(": ").append(value).append('\n');
                    });
                });
                payload.append(BatchConstants.CRLF);
            }

            if (item.getContent() != null) {
                payload.append(item.getContent()).append('\n');
            }
        });

        payload.append(boundary).append(BatchConstants.DOUBLE_DASH).append('\n');

        return payload.toString();
    }
}
