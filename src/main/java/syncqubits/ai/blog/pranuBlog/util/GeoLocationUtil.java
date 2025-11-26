package syncqubits.ai.blog.pranuBlog.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class GeoLocationUtil {

    /**
     * Get geolocation data from IP address
     * For production, integrate with a real GeoIP service like MaxMind or ip-api.com
     */
    public Map<String, String> getLocationFromIp(String ipAddress) {
        Map<String, String> location = new HashMap<>();

        // For localhost/private IPs, return default values
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1") ||
                ipAddress.startsWith("127.") || ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") || ipAddress.equals("unknown")) {
            location.put("country", "Unknown");
            location.put("city", "Unknown");
            location.put("region", "Unknown");
            return location;
        }

        // TODO: Integrate with real GeoIP service
        // Example using ip-api.com (free tier):
        // RestTemplate restTemplate = new RestTemplate();
        // String url = "http://ip-api.com/json/" + ipAddress;
        // Map response = restTemplate.getForObject(url, Map.class);

        // For now, return placeholder values
        location.put("country", "India"); // Default for demo
        location.put("city", "Delhi");
        location.put("region", "Delhi");

        log.debug("Geolocation for IP {}: {}", ipAddress, location);
        return location;
    }
}