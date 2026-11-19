package com.devlil0.tramposeditor.config;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthClientOrigin {
    public static final String CLIENT_IP_HEADER = "X-Trampos-Client-IP";
    private final Set<String> trustedProxies;

    AuthClientOrigin(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies.stream().map(value -> {
            String address = numericAddress(value.trim());
            if (address == null) throw new IllegalArgumentException("Proxy confiável deve ser um IP literal");
            return address;
        }).collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String peer = numericAddress(request.getRemoteAddr());
        if (peer == null) return "unknown";
        if (!trustedProxies.contains(peer)) return peer;
        var headers = request.getHeaders(CLIENT_IP_HEADER);
        if (!headers.hasMoreElements()) return peer;
        String forwarded = numericAddress(headers.nextElement().trim());
        return forwarded != null && !headers.hasMoreElements() ? forwarded : peer;
    }

    private static String numericAddress(String value) {
        if (value == null || value.length() > 45) return null;
        if (value.indexOf(':') >= 0) {
            if (!value.matches("[0-9a-fA-F:.]+")) return null;
            try {
                return InetAddress.getByName(value).getHostAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return null;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) return null;
        }
        return value;
    }
}
