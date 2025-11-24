package syncqubits.ai.blog.pranuBlog.mapper;

import org.mapstruct.*;
import syncqubits.ai.blog.pranuBlog.dto.request.CreatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.UpdatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.PostDetailResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.entity.Post;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "shareToken", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "isPublic", constant = "false")
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Post toEntity(CreatePostRequest request);

    @Mapping(target = "status", expression = "java(post.getStatus().name())")
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "metrics", ignore = true)
    PostResponse toPostResponse(Post post);

    @Mapping(target = "status", expression = "java(post.getStatus().name())")
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "isFavorite", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    PostDetailResponse toPostDetailResponse(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "shareToken", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isPublic", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdatePostRequest request, @MappingTarget Post post);
}