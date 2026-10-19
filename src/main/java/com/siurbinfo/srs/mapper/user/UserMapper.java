package com.siurbinfo.srs.mapper.user;

import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.repository.department.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final DepartmentRepository departmentRepository;

    public UserEntity toEntity(UserRequest dto){
        UserEntity user = new UserEntity();
        user.setEmail(dto.email());
        return user;
    }

    public UserEntity updateEntity(UserRequest dto, UserEntity entity){
        // Objects.equals porque os campos podem estar nulos no banco
        if (!Objects.equals(entity.getGivenName(), dto.givenName())){
            entity.setGivenName(dto.givenName());
        }
        if (!Objects.equals(entity.getFamilyName(), dto.familyName())){
            entity.setFamilyName(dto.familyName());
        }
        // getId() no proxy lazy nao dispara carregamento
        Long currentDepartmentId = entity.getDepartment() != null ? entity.getDepartment().getId() : null;
        if (!Objects.equals(currentDepartmentId, dto.idDepartment())){
            entity.setDepartment(resolveDepartment(dto.idDepartment()));
        }
        return entity;
    }

    public DepartmentEntity resolveDepartment(Long idDepartment){
        if (idDepartment == null) {
            return null;
        }
        return departmentRepository.findById(idDepartment)
                .orElseThrow(() -> new EntityNotFoundException("Não foi encontrado o departamento: " + idDepartment));
    }

}
