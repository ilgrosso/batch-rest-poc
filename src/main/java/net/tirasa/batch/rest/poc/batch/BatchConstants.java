package net.tirasa.batch.rest.poc.batch;

public final class BatchConstants {

    public static final String PREFER_HEADER = "Prefer";

    public static final String PREFERENCE_RESPOND_ASYNC = "respond-asyncF";

    public static final String PREFERENCE_APPLIED_HEADER = "Preference-Applied";

    public static final String DOUBLE_DASH = "--";

    public static final String CRLF = "\r\n";

    public static final String BOUNDARY_PARAMETER = "boundary";

    public static final String MULTIPART_MIXED = "multipart/mixed";

    public static String multipartMixedWith(final String boundary) {
        return MULTIPART_MIXED + ";" + BOUNDARY_PARAMETER + "=" + boundary;
    }

    private BatchConstants() {
        // private constructor for static utility class
    }
}
