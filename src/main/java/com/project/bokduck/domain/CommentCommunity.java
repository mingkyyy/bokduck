package com.project.bokduck.domain;

import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCommunity implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String text; // 댓글 내용

    @Column(nullable = false)
    private String nickname; // 댓글 쓴 사람 닉네임

    @ManyToOne
    private Community community; // 댓글 쓴 게시글

    private long parentId;

    private LocalDateTime regdate; // 작성일자
}
