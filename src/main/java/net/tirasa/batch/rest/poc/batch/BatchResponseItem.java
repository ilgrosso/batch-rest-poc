package net.tirasa.batch.rest.poc.batch;

public class BatchResponseItem extends BatchItem {

    private static final long serialVersionUID = -2163506313221985565L;

    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }
}
