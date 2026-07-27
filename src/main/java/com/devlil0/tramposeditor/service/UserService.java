package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.UserResponseDTO;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        return userMapper.toDTO(user);
    }
}
