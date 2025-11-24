package syncqubits.ai.blog.pranuBlog.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePostRequest {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    private String slug;

    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;

    private String excerpt;

    private String coverImageUrl;

    private Boolean allowComments;
}