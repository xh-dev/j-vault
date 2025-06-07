package me.xethh.tools.jvault.authserv;

public class SimpleAuthServerClient implements AuthServerClient {
    private final String url;

    public SimpleAuthServerClient(String url) {
        this.url = url;
    }

    @Override
    public String authServer() {
        return url;
    }
}
