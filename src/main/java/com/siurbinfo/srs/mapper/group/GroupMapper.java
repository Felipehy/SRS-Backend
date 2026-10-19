package com.siurbinfo.srs.mapper.group;

import com.siurbinfo.srs.dto.cognito.group.GroupRequest;
import com.siurbinfo.srs.entity.group.GroupEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    GroupEntity toEntity(GroupRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupName", conditionQualifiedByName = "preenchido")
    @Mapping(target = "groupCognito", conditionQualifiedByName = "preenchido")
    @Mapping(target = "colorGroup", conditionQualifiedByName = "preenchido")
    @Mapping(target = "description", conditionQualifiedByName = "preenchido")
    void updateGroup(GroupRequest dto, @MappingTarget GroupEntity entity);

    @Condition
    @Named("preenchido")
    default boolean preenchido(String value) {
        return value != null && !value.isBlank();
    }

}
