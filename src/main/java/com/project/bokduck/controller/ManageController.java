package com.project.bokduck.controller;

import com.google.gson.JsonObject;
import com.project.bokduck.domain.*;
import com.project.bokduck.repository.*;
import com.project.bokduck.service.ReviewService;
import com.project.bokduck.specification.ReviewSpecs;
import com.project.bokduck.util.CurrentMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage")
@Slf4j
@RequiredArgsConstructor
public class ManageController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    /**
     * @param member   로그인한 사용자 정보
     * @param model
     * @param pageable 리스트 페이징 처리
     * @return 관리자페이지
     * @author 미리
     */
    @GetMapping("")
    public String ManagePage(@CurrentMember Member member, Model model,
                             @PageableDefault(size = 10) Pageable pageable) {

        Specification<Review> spec = ReviewSpecs.searchReviewStatus(ReviewStatus.WAIT);
        Page<Review> reviewList = reviewRepository.findAll(spec, pageable);
        model.addAttribute("reviewList", reviewList);
        if (member == null || member.getMemberType() != MemberType.ROLE_MANAGE) {
            return "redirect:/login";
        }
        model.addAttribute("member", member);
        int startPage = Math.max(1, reviewList.getPageable().getPageNumber() - 10);
        int endPage = Math.min(reviewList.getTotalPages(), reviewList.getPageable().getPageNumber() + 10);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        log.info("reviewList : {}", reviewList);
        return "manage/list";
    }


    /**
     * @param id     해당 게시글의 id
     * @param member 로그인한 사용자 정보
     * @return 승인 버튼을 눌렀을 때의 AJAX
     * @author 미리
     */
    @GetMapping("/list/approval")
    @ResponseBody
    public String reviewApproval(Long id, @CurrentMember Member member) {
        String resultCode = "";
        String message = "";


        switch (reviewService.addApproval(member, id)) {
            case ERROR_AUTH:
                resultCode = "error.auth";
                message = "로그인이 필요한 서비스입니다.";
                break;
            case ERROR_INVALID:
                resultCode = "error.invalid";
                message = "삭제된 게시물 입니다.";
                break;
            case DUPLICATE:
                resultCode = "duplicate";
                message = "이미 승인한 게시물입니다.";
                break;
            case OK:
                resultCode = "ok";
                message = "승인 완료!";
                break;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("resultCode", resultCode);
        jsonObject.addProperty("message", message);

        return jsonObject.toString();
    }

    /**
     * @param id     해당 게시글의 id
     * @param member 로그인한 사용자 정보
     * @return 거부 버튼을 눌렀을 때의 AJAX
     * @author 미리
     */
    @GetMapping("/list/refusal")
    @ResponseBody
    public String reviewRefusal(Long id, @CurrentMember Member member) {
        String resultCode = "";
        String message = "";


        switch (reviewService.addRefusal(member, id)) {
            case ERROR_AUTH:
                resultCode = "error.auth";
                message = "로그인이 필요한 서비스입니다.";
                break;
            case ERROR_INVALID:
                resultCode = "error.invalid";
                message = "삭제된 게시물 입니다.";
                break;
            case DUPLICATE:
                resultCode = "duplicate";
                message = "이미 거부한 게시물입니다.";
                break;
            case OK:
                resultCode = "ok";
                message = "거부 완료!";
                break;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("resultCode", resultCode);
        jsonObject.addProperty("message", message);

        log.info("jsonObject.toString() : {}", jsonObject.toString());

        return jsonObject.toString();
    }

}
