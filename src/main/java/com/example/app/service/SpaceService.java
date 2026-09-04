package com.example.app.service;

import com.example.app.dto.SpaceDtos.SpaceRequest;
import com.example.app.dto.SpaceDtos.SpaceResponse;
import com.example.app.entity.Space;
import com.example.app.entity.SpaceType;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.SpaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SpaceService {

  private final SpaceRepository spaceRepository;

  public SpaceService(SpaceRepository spaceRepository) {
    this.spaceRepository = spaceRepository;
  }

  public SpaceResponse create(SpaceRequest request) {
    Space space = new Space();
    applyRequest(space, request);
    Space saved = spaceRepository.save(space);
    return toResponse(saved);
  }

  public Page<SpaceResponse> list(SpaceType type, Pageable pageable) {
    if (type != null) {
      return spaceRepository.findByDeletedFalseAndType(type, pageable).map(this::toResponse);
    }
    return spaceRepository.findByDeletedFalse(pageable).map(this::toResponse);
  }

  public SpaceResponse get(Long id) {
    return toResponse(findEntity(id));
  }

  public SpaceResponse update(Long id, SpaceRequest request) {
    Space space = findEntity(id);
    applyRequest(space, request);
    Space saved = spaceRepository.save(space);
    return toResponse(saved);
  }

  public void delete(Long id) {
    Space space = findEntity(id);
    space.setDeleted(true);
    space.setActive(false);
    spaceRepository.save(space);
  }

  public Space findEntity(Long id) {
    return spaceRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + id));
  }

  private void applyRequest(Space space, SpaceRequest request) {
    space.setName(request.getName());
    space.setType(request.getType());
    space.setCapacity(request.getCapacity());
    space.setHourlyRate(request.getHourlyRate());
    if (request.getIsActive() != null) {
      space.setActive(request.getIsActive());
    }
  }

  private SpaceResponse toResponse(Space space) {
    SpaceResponse response = new SpaceResponse();
    response.setId(space.getId());
    response.setName(space.getName());
    response.setType(space.getType());
    response.setCapacity(space.getCapacity());
    response.setHourlyRate(space.getHourlyRate());
    response.setActive(space.isActive());
    response.setCreatedAt(space.getCreatedAt());
    response.setUpdatedAt(space.getUpdatedAt());
    return response;
  }
}
