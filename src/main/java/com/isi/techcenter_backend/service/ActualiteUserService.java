package com.isi.techcenter_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isi.techcenter_backend.model.ActualiteUserResponse;
import com.isi.techcenter_backend.repository.ActualiteRepository;

@Service
public class ActualiteUserService {

    private final ActualiteRepository actualiteRepository;
    private final MinioStorageService minioStorageService;

    public ActualiteUserService(
            ActualiteRepository actualiteRepository,
            MinioStorageService minioStorageService) {
        this.actualiteRepository = actualiteRepository;
        this.minioStorageService = minioStorageService;
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
                        actualite.getModerateur().getUserId(),
                        minioStorageService.getActualitePhotoPresignedUrl(actualite.getPhotoPath())))
                .toList();
    }
}
