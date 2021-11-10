package com.project.bokduck.repository;


import com.project.bokduck.domain.File;
import com.project.bokduck.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> ,JpaSpecificationExecutor<File> {
}
