package syncqubits.ai.blog.pranuBlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;
import syncqubits.ai.blog.pranuBlog.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard and analytics")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get dashboard", description = "Get analytics dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard retrieved")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/dashboard - User: {}", userId);
        DashboardResponse response = dashboardService.getDashboard(userId);
        return ResponseEntity.ok(response);
    }
}