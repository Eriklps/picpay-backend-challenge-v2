package com.example.picpaybackendchallenge.authorization;

public record Authorization(
        String message
) {
    public boolean isAuthorized() {
        return message.equals("Authorized");
    }
}
