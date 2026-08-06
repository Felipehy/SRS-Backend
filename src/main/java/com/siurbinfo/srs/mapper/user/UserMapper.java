package com.siurbinfo.srs.mapper.user;

import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(UserRequest dto){
        UserEntity user = new UserEntity();
        user.setEmail(dto.email());
        return user;
    }

}
