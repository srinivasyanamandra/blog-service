package syncqubits.ai.blog.pranuBlog.service;

import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(Long authorId);
}