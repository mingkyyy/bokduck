package com.project.bokduck.repository;


import com.project.bokduck.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.swing.plaf.metal.MetalMenuBarUI;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    /**
     * @param writer
     * @param pageable
     * @return
     * @Author MunKyoung
     * review 에서 맴버형을 가지고 조회
     */
    Page<Review> findAllByWriter(Member writer, Pageable pageable);

    List<Review> findByWriter(Long id);

    Review findAllById(Long id);
}

