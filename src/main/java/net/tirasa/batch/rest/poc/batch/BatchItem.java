package net.tirasa.batch.rest.poc.batch;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class BatchItem implements Serializable {

    private static final long serialVersionUID = -1393976266651766259L;

    private final Map<String, List<Object>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private String content;

    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, List<Object>> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
