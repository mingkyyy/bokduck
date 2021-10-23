package com.project.bokduck.domain;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter @Setter
@ToString
@EqualsAndHashCode(exclude = "review")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCategory {
    @Id @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "reviewCategory", cascade = CascadeType.ALL)
    private Review review;

    @Enumerated(EnumType.STRING)
    private RoomSize roomSize ;

    @Enumerated(EnumType.STRING)
    private Structure structure;

    @Enumerated(EnumType.STRING)
    private Payment payment;

    private String traffic;

    private String convenient;

    private String welfare;

    private String electronicDevice;

}
