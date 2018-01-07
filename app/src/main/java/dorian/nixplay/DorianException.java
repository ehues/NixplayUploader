package dorian.nixplay;

import okhttp3.Response;

class DorianException extends Exception {

    private final Response resp;

    DorianException(String reason, Response resp) {
        super(reason);
        this.resp = resp;
    }

    public Response getResponse() {
        return resp;
    }
}
