package syncqubits.ai.blog.pranuBlog.service;

import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;
import java.time.LocalDateTime;

public interface DashboardService {
    DashboardResponse getDashboard(
            Long authorId,
            String search,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean favoritesOnly,
            String sortBy,
            int page,
            int size
    );
}