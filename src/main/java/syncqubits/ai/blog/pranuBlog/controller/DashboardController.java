package syncqubits.ai.blog.pranuBlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.service.DashboardService;

import java.time.LocalDateTime;

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
    @Operation(summary = "Get dashboard", description = "Get analytics dashboard with filters and pagination")
    @ApiResponse(responseCode = "200", description = "Dashboard retrieved")
    public ResponseEntity<DashboardResponse> getDashboard(
            Authentication authentication,

            @Parameter(description = "Search keyword in title or content")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by status: DRAFT, PUBLISHED, ARCHIVED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Start date for filtering posts")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @Parameter(description = "End date for filtering posts")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @Parameter(description = "Filter only favorite posts")
            @RequestParam(required = false, defaultValue = "false") Boolean favoritesOnly,

            @Parameter(description = "Sort by: RECENT, TOP_VIEWS, TOP_LIKES, TOP_COMMENTS")
            @RequestParam(required = false, defaultValue = "RECENT") String sortBy,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/dashboard - User: {}, search: {}, status: {}, sortBy: {}, page: {}, size: {}",
                userId, search, status, sortBy, page, size);

        DashboardResponse response = dashboardService.getDashboard(
                userId, search, status, fromDate, toDate, favoritesOnly, sortBy, page, size
        );
        return ResponseEntity.ok(response);
    }
}