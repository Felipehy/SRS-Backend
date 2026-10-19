package com.siurbinfo.srs.repository.group;

import com.siurbinfo.srs.entity.group.GroupEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface GroupRepository extends JpaRepository<GroupEntity,Long> {

    @Query(value = """
    select  tg.group_cognito as groupCognito,
            tg.group_name as groupName,
            tg.description as description,
            tg.color_group as colorGroup
    from tbl_groups tg
    """, nativeQuery = true)
    List<Map<String,String>> getGroups();

    // Derived queries: o Spring Data monta o SQL a partir do nome do metodo e o
    // Hibernate materializa a entidade inteira. Escrever essas duas como
    // nativeQuery exigiria devolver todas as colunas mapeadas (inclusive id) com
    // o nome exato da coluna — os aliases em camelCase quebram o mapeamento.
    GroupEntity getGroupByGroupCognito(String groupCognito);

    @Transactional
    void deleteByGroupCognito(String groupCognito);

}
