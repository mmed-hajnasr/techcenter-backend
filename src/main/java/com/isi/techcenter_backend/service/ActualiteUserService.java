package com.isi.techcenter_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.model.ActualiteUserResponse;
import com.isi.techcenter_backend.repository.ActualiteRepository;

@Service
public class ActualiteUserService {

    private final ActualiteRepository actualiteRepository;

    public ActualiteUserService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    @Transactional(readOnly = true)
    public List<ActualiteUserResponse> listActualites() {
        return actualiteRepository.findAllForUser()
                .stream()
                .map(actualite -> new ActualiteUserResponse(
                        actualite.getActualiteId(),
                        actualite.getTitre(),
                        actualite.getContenu(),
                        actualite.getDatePublication(),
                        actualite.getEstEnAvant(),
                        actualite.getModerateur().getUserId()))
                .toList();
    }
}
