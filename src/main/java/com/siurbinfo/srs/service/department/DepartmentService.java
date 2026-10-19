package com.siurbinfo.srs.service.department;

import com.siurbinfo.srs.dto.department.DepartmentRequestDTO;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.DepartmentInUseException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.repository.department.DepartmentRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponseDTO> getAllDepartment(){
        return repository.getAllDepartment().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public DepartmentResponseDTO createDepartment(DepartmentRequestDTO dto){
        DepartmentEntity entity = mapper.toEntity(dto);
        repository.save(entity);
        return toResponseDTO(entity);
    }

    @Transactional
    public void updateDepartment(DepartmentRequestDTO dto, Long id){
        DepartmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new DepartmentIdNotFoundedException("Não foi encontrado o departamento: " + id));
        mapper.updateEntity(dto,entity);
        repository.save(entity);
    }

    private DepartmentResponseDTO toResponseDTO(DepartmentEntity entity){
        RoomEntity room = entity.getRoom();
        return new DepartmentResponseDTO(
                entity.getName(),
                room != null ? room.getFloor() : null,
                room != null ? room.getId() : null,
                entity.getId()
        );
    }

    @Transactional
    public void deleteDepartment(Long id){
        // deleteById e no-op quando o id nao existe: checamos antes para devolver 404
        if (!repository.existsById(id)) {
            throw new DepartmentIdNotFoundedException("Não foi encontrado o departamento: " + id);
        }
        // sem isso a FK fk_department_user estoura como 500 no meio do commit
        long usuariosVinculados = userRepository.countByIdDepartment(id);
        if (usuariosVinculados > 0) {
            throw new DepartmentInUseException(
                    "O departamento ainda possui " + usuariosVinculados + " usuário(s) vinculado(s)");
        }
        repository.deleteById(id);
    }
}
