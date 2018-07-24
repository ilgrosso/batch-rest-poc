package net.tirasa.batch.rest.poc.batch;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.springframework.util.CollectionUtils;

public class BatchPayloadGenerator {

    private static final String HTTP_1_1 = "HTTP/1.1";

    public static <T extends BatchItem> String generate(final List<T> items, final String boundary) {
        StringBuilder payload = new StringBuilder();

        items.forEach(item -> {
            payload.append(boundary).append(BatchConstants.CRLF);
            payload.append(HttpHeaders.CONTENT_TYPE).append(": ").append("application/http").append('\n');
            payload.append("Content-Transfer-Encoding: binary").append('\n');
            payload.append(BatchConstants.CRLF);

            if (item instanceof BatchRequestItem) {
                BatchRequestItem bri = BatchRequestItem.class.cast(item);
                payload.append(bri.getMethod()).append(' ').append(bri.getRequestURI());
                if (bri.getQueryString() != null) {
                    payload.append('?').append(bri.getQueryString());
                }
                payload.append(' ').append(HTTP_1_1).append('\n');
            }

            if (item instanceof BatchResponseItem) {
                BatchResponseItem bri = BatchResponseItem.class.cast(item);
                payload.append(HTTP_1_1).append(' ').
                        append(bri.getStatus()).append(' ').
                        append(Response.Status.fromStatusCode(bri.getStatus()).getReasonPhrase()).
                        append('\n');
            }

            if (!CollectionUtils.isEmpty(item.getHeaders())) {
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
