package net.tirasa.batch.rest.poc.batch;

public class BatchRequestItem extends BatchItem {

    private static final long serialVersionUID = -986002485818968262L;

    private String method;

    private String requestURI;

    private String queryString;

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }
}
