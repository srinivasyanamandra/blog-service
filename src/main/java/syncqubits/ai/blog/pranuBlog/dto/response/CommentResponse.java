package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;
import syncqubits.ai.blog.pranuBlog.dto.nested.CommentEntryDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private String message;
    private CommentEntryDTO comment;
}