package syncqubits.ai.blog.pranuBlog.dto.nested;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntryDTO {
    private String commentId;
    private Long userId;
    private String guestName;
    private String content;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentEntryDTO> replies = new ArrayList<>();
}