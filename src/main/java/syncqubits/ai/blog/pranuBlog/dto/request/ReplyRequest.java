package syncqubits.ai.blog.pranuBlog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyRequest {

    @NotBlank(message = "Parent comment ID is required")
    private String parentCommentId;

    private Long userId;

    @NotBlank(message = "Guest name is required")
    private String guestName;

    private String guestIdentifier;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters")
    private String content;
}