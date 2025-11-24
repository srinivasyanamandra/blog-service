package syncqubits.ai.blog.pranuBlog.mapper;

import org.mapstruct.*;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;
import syncqubits.ai.blog.pranuBlog.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "AUTHOR")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "posts", ignore = true)
    User toEntity(SignupRequest request);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "message", ignore = true)
    AuthResponse toAuthResponse(User user);
}