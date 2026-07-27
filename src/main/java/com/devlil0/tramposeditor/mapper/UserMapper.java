package com.devlil0.tramposeditor.mapper;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.dto.UserResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    public UserResponseDTO toDTO(UserEntity user){
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                Set.copyOf(user.getRoles()),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
