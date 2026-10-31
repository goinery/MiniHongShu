package com.hongshu.web.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hongshu.common.utils.ip.AddressUtils;
import com.hongshu.common.utils.ip.IpUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Community profile locations are derived from the client IP, never from an editable address field. */
public final class WebClientLocation {
    private static final String UNKNOWN = "未知";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<String, CachedLocation> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedLocation> eldest) {
            return size() > 1024;
        }
    });

    private WebClientLocation() { }

    public static String current() {
        return resolve(IpUtils.getIpAddr());
    }

    public static String resolve(String ip) {
        // Literal addresses only; do not resolve hostnames supplied in forwarding headers.
        if (ip == null || !ip.matches("[0-9a-fA-F:.]+") || (!ip.contains(".") && !ip.contains(":"))) return UNKNOWN;
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] bytes = address.getAddress();
            boolean privateV6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
            boolean sharedV4 = bytes.length == 4 && (bytes[0] & 0xff) == 100 && (bytes[1] & 0xff) >= 64 && (bytes[1] & 0xff) <= 127;
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isMulticastAddress() || privateV6 || sharedV4) return "内网IP";
            ip = address.getHostAddress();
        } catch (Exception ignored) {
            return UNKNOWN;
        }
        CachedLocation cached = CACHE.get(ip);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) return cached.value();
        String location = UNKNOWN;
        try {
            URI uri = URI.create(AddressUtils.IP_URL + "?ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8) + "&json=true");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(Charset.forName("GBK")));
            if (response.statusCode() == 200) {
                JSONObject body = JSON.parseObject(response.body());
                String province = value(body, "pro");
                String city = value(body, "city");
                String detected = (province + (province.equals(city) ? "" : " " + city)).trim();
                if (detected.isEmpty()) detected = value(body, "addr");
                if (!detected.isEmpty()) location = detected.substring(0, Math.min(50, detected.length()));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Geolocation must not prevent login or saving a profile.
        }
        long ttl = UNKNOWN.equals(location) ? 60_000L : 3_600_000L;
        CACHE.put(ip, new CachedLocation(location, System.currentTimeMillis() + ttl));
        return location;
    }

    private static String value(JSONObject object, String key) {
        String text = object == null ? null : object.getString(key);
        return text == null || "null".equalsIgnoreCase(text) ? "" : text.trim();
    }

    private record CachedLocation(String value, long expiresAt) { }
}
