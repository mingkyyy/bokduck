package com.project.bokduck.controller;


import com.google.gson.JsonObject;
import com.project.bokduck.domain.*;
import com.project.bokduck.repository.*;
import com.project.bokduck.service.CommunityService;
import com.project.bokduck.service.MainpageService;
import com.project.bokduck.service.MemberService;
import com.project.bokduck.service.PassEmailService;
import com.project.bokduck.util.CommunityFormVo;
import com.project.bokduck.domain.Member;
import com.project.bokduck.domain.Review;
import com.project.bokduck.repository.MemberRepository;
import com.project.bokduck.service.ReviewService;
import com.project.bokduck.util.CurrentMember;
import com.project.bokduck.util.MemberChangeDto;
import com.project.bokduck.validation.JoinFormValidator;
import com.project.bokduck.validation.JoinFormVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final CommunityRepository communityRepository;
    private final PassEmailService passEmailService;
    private final TagRepository tagRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final ReviewCategoryRepository reviewCategoryRepository;
    private final PlatformTransactionManager transactionManager;
    private final ImageRepository imageRepository;
    private final FileRepository fileRepository;
    private final MainpageService mainpageService;
    private final PasswordEncoder passwordEncoder;
    private final CommentCommunityRepository commentCommunityRepository;
    private final CommentReviewRepository commentReviewRepository;

    /**
     * 임의의 리뷰글 및 커뮤니티글 생성
     *
     * @author 미리
     */
    @PostConstruct
    @DependsOn("memberRepository")
    @Transactional
    public void createReviewsAndCommunities() {
        //리뷰글 생성
        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(
                    TransactionStatus status) {

                Long[] array = {1L, 2l};

                // 태그 만들어두기
                List<Tag> tagList = new ArrayList<>(); // 임시태그 담아보자
                String[] tagNameList = {"넓음", "깨끗함", "벌레없음"};

                for (String s : tagNameList) {
                    Tag tag = new Tag();
                    tag.setTagName(s);
                    tagList.add(tag);
                }

                tagRepository.saveAll(tagList);

                // 리뷰게시글을 만들어보자
                List<Review> reviewList = new ArrayList<>();
                ReviewCategory category = null;

                for (int i = 0; i < 50; ++i) {
                    category = new ReviewCategory();
                    if (i <= 24) {
                        category.setRoomSize(RoomSize.ONEROOM);
                        category.setStructure(Structure.VILLA);
                    } else {
                        category.setRoomSize(RoomSize.TWOROOM);
                        log.info("????");
                    }
                    category = reviewCategoryRepository.save(category);

                    Member member = memberRepository
                            .findById(array[(int) (Math.random() * array.length)]).orElseThrow();

                    Review review = Review.builder()
                            .postName((i + 1) + "번 게시물")
                            .postContent("어쩌구저쩌구")
                            .writer(member)
                            .comment("무난하다")
                            .regdate(LocalDateTime.now())
                            .hit((int) (Math.random() * 10))
                            .star((int) (Math.random() * 5) + 1)
                            .address("서울특별시 마포구 월드컵로34길 14")
                            .detailAddress("XX빌라")
                            .extraAddress("연희동")
                            .reviewStatus(ReviewStatus.COMPLETE)
//                            .reviewCategory(category)
                            .build();
                    review.setReviewCategory(reviewCategoryRepository.findById((long) (i + 6)).get());
                    reviewList.add(review);
                }
                reviewRepository.saveAll(reviewList);

                // 태그 포스트에 넣기
                List<Tag> tag1 = tagRepository.findAll();
                List<Post> tagPostList = postRepository.findAll();
                for (Tag t : tag1) {
                    t.setTagToPost(tagPostList);
                }

                // 포토리뷰 만들어두기
                List<Image> imageList = new ArrayList<>();
                String[] imageNameList = {"photo_1.jpg", "photo_2.jpg"};
                String[] imagePathList = {"/images/photo_1.jpg", "/images/photo_2.jpg"};

                for (int i = 0; i < 2; ++i) {
                    Image image = new Image();
                    image.setImageName(imageNameList[i]);
                    image.setImagePath(imagePathList[i]);
                    imageList.add(image);
                }

                imageRepository.saveAll(imageList);

                // 포토리뷰 포스트에 넣기
                List<Image> image1 = imageRepository.findAll();
                Post post = postRepository.findById(105l).orElseThrow();
                for (Image i : image1) {
                    i.setImageToPost(post);
                }

                //계약서 만들어두기
                List<File> fileList = new ArrayList<>();
                String[] fileNameList = {"floating1.png", "floating2.png"};
                String[] filePathList = {"/images/floating1.png", "/images/floating3.png"};

                for (int i = 0; i < 2; ++i) {
                    File file = new File();
                    file.setFilePath(filePathList[i]);
                    fileList.add(file);
                }

                fileRepository.saveAll(fileList);

                // 계약서 포스트에 넣기
                List<File> file1 = fileRepository.findAll();
                for (File f : file1) {
                    f.setFileToPost(post);
                }

                // 멤버 like 만들기
                Member member = memberRepository.findById(1l).orElseThrow();
                List<Post> likePostList = new ArrayList<>();
                Post post1 = postRepository.findById(103l).orElseThrow();
                likePostList.add(post1);
                member.setLikes(likePostList);

                member = memberRepository.findById(2l).orElseThrow();
                likePostList = postRepository.findAll();
                member.setLikes(likePostList);

                //리뷰 댓글 만들어놓기
                for (Review commu : reviewList) {
                    Member member1 = memberRepository.findById(array[(int) (Math.random() * array.length)]).orElseThrow();
                    Member member2 = memberRepository.findById(array[(int) (Math.random() * array.length)]).orElseThrow();

                    CommentReview comment1 = CommentReview.builder()
                            .text("댓글1 본문입니다... 리뷰 잘 봤습니다.")
                            .nickname(member1.getNickname())
                            .review(commu)
                            .parentId(-1l)
                            .regdate(LocalDateTime.now())
                            .build();

                    CommentReview comment2 = CommentReview.builder()
                            .text("댓글2 본문입니다... 좋은 리뷰였습니다.")
                            .nickname(member2.getNickname())
                            .review(commu)
                            .parentId(-1l)
                            .regdate(LocalDateTime.now())
                            .build();

                    comment1 = commentReviewRepository.save(comment1);
                    commentReviewRepository.save(comment2);

                    CommentReview comment3 = CommentReview.builder()
                            .text("댓글1의 대댓글1입니다... 저도 그렇게 생각합니다.")
                            .nickname(member1.getNickname())
                            .review(commu)
                            .parentId(comment1.getId())
                            .regdate(LocalDateTime.now())
                            .build();

                    CommentReview comment4 = CommentReview.builder()
                            .text("댓글1의 대댓글2입니다... 하하하.")
                            .nickname(member2.getNickname())
                            .review(commu)
                            .parentId(comment1.getId())
                            .regdate(LocalDateTime.now())
                            .build();

                    commentReviewRepository.save(comment3);
                    commentReviewRepository.save(comment4);
                }
            }
        });

        //커뮤니티글 생성
        TransactionTemplate tmpl2 = new TransactionTemplate(transactionManager);
        tmpl2.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Long[] arry = {1L, 2L};
                CommunityCategory[] categories = {CommunityCategory.TIP, CommunityCategory.EAT
                        , CommunityCategory.INTERIOR, CommunityCategory.BOARD};
                List<Community> communityList = new ArrayList<>();
                LocalDateTime localDateTime = LocalDateTime.now();

                List<Tag> tagList = new ArrayList<>(); // 임시태그 담아보자
                String[] tagNameList = {"태그1", "태그2", "태그3"};

                for (int i = 0; i < tagNameList.length; ++i) {
                    Tag tag = new Tag();
                    tag.setTagName(tagNameList[i]);
                    tagList.add(tag);
                }
                tagRepository.saveAll(tagList);

                for (int i = 0; i < 300; i++) {
                    Member member = memberRepository.findById(arry[(int) (Math.random() * arry.length)]).orElseThrow();
                    communityList.add(Community.builder()
                            .postName(i + "번 제목입니다.")
                            .postContent(i + "번 내용입니다.")
                            .writer(member)
                            .hit((int) ((Math.random() * 50) + 1))
                            .likeCount((int) (Math.random() * 100))
                            .regdate(localDateTime)
                            .communityCategory(categories[(int) (Math.random() * categories.length)])
                            .build());
                }
                communityRepository.saveAll(communityList);

                List<Tag> tag2 = tagRepository.findAll();
                List<Post> tagPostList2 = postRepository.findAll();
                for (Tag t : tag2) {
                    t.setTagToPost(tagPostList2);
                }

                //커뮤니티 댓글 만들어놓기
                for (Community commu : communityList) {
                    Member member1 = memberRepository.findById(arry[(int) (Math.random() * arry.length)]).orElseThrow();
                    Member member2 = memberRepository.findById(arry[(int) (Math.random() * arry.length)]).orElseThrow();

                    CommentCommunity comment1 = CommentCommunity.builder()
                            .text("댓글1 본문입니다... 리뷰 잘 봤습니다.")
                            .nickname(member1.getNickname())
                            .community(commu)
                            .parentId(-1L)
                            .regdate(LocalDateTime.now())
                            .build();

                    CommentCommunity comment2 = CommentCommunity.builder()
                            .text("댓글2 본문입니다... 좋은 리뷰였습니다.")
                            .nickname(member2.getNickname())
                            .community(commu)
                            .parentId(-1l)
                            .regdate(LocalDateTime.now())
                            .build();

                    comment1 = commentCommunityRepository.save(comment1);
                    commentCommunityRepository.save(comment2);

                    CommentCommunity comment3 = CommentCommunity.builder()
                            .text("댓글1의 대댓글1입니다... 저도 그렇게 생각합니다.")
                            .nickname(member1.getNickname())
                            .community(commu)
                            .parentId(comment1.getId())
                            .regdate(LocalDateTime.now())
                            .build();

                    CommentCommunity comment4 = CommentCommunity.builder()
                            .text("댓글1의 대댓글2입니다... 하하하.")
                            .nickname(member2.getNickname())
                            .community(commu)
                            .parentId(comment1.getId())
                            .regdate(LocalDateTime.now())
                            .build();

                    commentCommunityRepository.save(comment3);
                    commentCommunityRepository.save(comment4);
                }
            }
        });
    }


    @InitBinder("joinFormVo")
    protected void initBinder(WebDataBinder dataBinder) {
        dataBinder.addValidators(new JoinFormValidator(memberRepository));
    }

    /**
     * "/" 요청을 받으면 메인페이지를 불러온다
     *
     * @param model 리턴되는 페이지로 애트리뷰트 전달
     * @return index.html
     * @author 이선주
     */
    @RequestMapping("/")
    public String index(Model model) {

        //리뷰 인기게시글 불러오기
        Page<Review> reviewList1 = mainpageService.getReviewList(PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "likeCount")));
        model.addAttribute("reviewList1", reviewList1);
        Page<Review> reviewList2 = mainpageService.getReviewList(PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "likeCount")));
        model.addAttribute("reviewList2", reviewList2);
        Page<Review> reviewList3 = mainpageService.getReviewList(PageRequest.of(2, 3, Sort.by(Sort.Direction.DESC, "likeCount")));
        model.addAttribute("reviewList3", reviewList3);

        //커뮤니티 인기게시글 불러오기
        Page<Community> communityList = mainpageService.getCommunityList(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "likeCount")));
        model.addAttribute("communityList", communityList);

        //자취방꿀팁 불러오기
        Page<Community> communityTipList = mainpageService.getCommunityTipList(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("communityTipList", communityTipList);

        return "index";
    }

    /**
     * 회원가입시 핸드폰 번호 인증 보내기
     *
     * @param phoneNumber 인증보낼 핸드폰 번호
     * @return 인증 번호
     * @author 민경
     */
    @GetMapping("/check/sendSMS")
    @ResponseBody
    public String sendSMS(String phoneNumber) {

        Random rand = new Random();
        String numStr = "";
        for (int i = 0; i < 4; i++) {
            String ran = Integer.toString(rand.nextInt(10));
            numStr += ran;
        }
        System.out.println("수신자 번호 : " + phoneNumber);
        System.out.println("인증번호 : " + numStr);
        memberService.certifiedPhoneNumber(phoneNumber, numStr);
        return numStr;

    }

    /**
     * @return 로그인 페이지
     * @author 미리
     */
    @GetMapping("/login")
    public String login() {
        return "member/login";
    }

    /**
     * 회원가입 버튼이 눌리면(요청이 들어오면) 회원가입 폼 페이지를 불러온다
     *
     * @param model 리턴되는 페이지로 애트리뷰트 전달
     * @return /member/signup.html
     * @author 이선주
     */
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("joinFormVo", new JoinFormVo());
        return "/member/signup";
    }

    /**
     * 회원가입 폼을 입력 후 가입 버튼을 눌렀을 때, DB에 사용자가 입력한 정보들을 저장하고 강제 로그인해준다
     *
     * @param joinFormVo 회원가입 입력 폼을 위한 VO
     * @param errors     일어날 수 있는 회원가입 에러들
     * @return 메인페이지로 되돌아간다
     * @author 이선주
     */
    @PostMapping("/signup")
    public String signupSubmit(@Valid JoinFormVo joinFormVo, Errors errors) {
        if (errors.hasErrors()) {
            log.info("회원가입 에러 : {}", errors.getAllErrors());
            return "/member/signup";
        }
        memberService.processNewMember(joinFormVo);
        return "redirect:/";
    }

    /**
     * 비밀번호 찾기 버튼 누르면 비밀번호 찾기 페이지를 불러온다.
     *
     * @return member/password.html
     * @author 민경
     */
    @GetMapping("/password")
    public String password() {
        return "member/password";
    }

    @ResponseBody
    @PostMapping("/password")
    public Object passwordSubmit(@RequestParam("username") String username) {
        Map<String, Object> map = new HashMap<>();
        map.put("result", memberService.passwordSubmit(username));

        return map;
    }

    @PostMapping("/mypage/change")
    @ResponseBody
    public Object memberChange(@RequestBody MemberChangeDto memberChangeDto, @CurrentMember Member member) {
        Map<String, String> map = new HashMap<>();

        map.put("nicknameResult", memberService.changeNickname(member, memberChangeDto.getNickname()));
        map.put("addressResult", memberService.changeAddress(memberChangeDto, member));

        return map;
    }

    /**
     * 비밀번호 변경 버튼을 클릭 했을때 보여주는 뷰
     *
     * @param model
     * @param member 현재 로그인한 회원 정보
     * @return 성공 -> /.html 이동 실패 -> member/password-change.html
     * @author 민경
     */
    @RequestMapping("/password/change")
    public String passwordChange(Model model, @CurrentMember Member member) {
        if (member == null) {
            return "redirect:/";
        }
        model.addAttribute("member", "member");
        return "member/password-change";
    }


    /**
     * 마이페이지를 클릭하면 마이페이지 이동
     *
     * @param model
     * @param member   현재 로그인한 회원
     * @param pageable
     * @param check
     * @param page
     * @return /member/mypage.html
     * @author 민경,  MunKyoung
     */
    @GetMapping("/mypage")
    public String mypage(Model model,
                         @CurrentMember Member member,
                         @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                         @RequestParam(required = false, defaultValue = "all") String check,
                         @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        Review review;

        if (member == null) {
            return "redirect:/";
        } else {
            member = memberRepository.findById(member.getId()).orElseThrow();
        }
        review = reviewRepository.getById(member.getId());
        model.addAttribute("member", member);
        Page<Community> communityList = communityRepository.findAllByWriter(member, pageable);
        Page<Review> reviewList = reviewRepository.findAllByWriter(member, pageable);
        model.addAttribute("communityMaxPage", communityList.getSize());
        model.addAttribute("reviewMaxPage", reviewList.getSize());
        model.addAttribute("check", check);
        model.addAttribute("state", "all");
        model.addAttribute("communityList", communityList);
        model.addAttribute("reviewList", reviewList);
        return "/member/mypage";
    }

    /**
     * 현재 비밀번호, 새로운 비밀번호를 입력받아 비밀번호 변경한다.
     *
     * @param member  현재 로그인한 회원
     * @param oldpass 현재 비밀번호
     * @param pass    새로운 비밀번호
     * @param repass  새로운 비밀번호 확인
     * @return 실패 -> 실패 메세지를 띄어준다, 성공 -> db상에 비밀번호 변경한다.
     * @author 민경
     */
    @ResponseBody
    @GetMapping("/password/change/result")
    public String passwordChangeResult(@CurrentMember Member member, String oldpass, String pass, String repass) {
        String message = "";
        if (member == null) {
            message = "로그인 되어있지 않습니다. 다시 로그인해주세요.";
        }
        if (!oldpass.isEmpty() && !pass.isEmpty() && !repass.isEmpty()) {
            if (passwordEncoder.matches(oldpass, member.getPassword())) {
                if (pass.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[#?!@$%^&*-]).{8,}$")) {
                    if (pass.equals(repass)) {
                        member.setPassword(passwordEncoder.encode(pass));
                        memberRepository.save(member);
                        message = "비밀번호 변경 완료 되었습니다.";
                    } else {
                        message = "새로운 비밀번호가 서로 다릅니다. 다시 시도해주세요.";
                    }
                } else {
                    message = "패스워드는 영문자, 숫자, 특수기호를 조합하여 최소 8자 이상을 입력하셔야 합니다";
                }
            } else {
                message = "현재 비밀번호가 다릅니다.";
            }
        } else {
            message = "모두 입력하셔야 합니다.";
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        return jsonObject.toString();
    }

    /**
     * 작성한 글, 댓글, 회원 정보를 모두 db상에서 삭제한다.
     *
     * @param member 현재 로그인한 회원
     * @return 성공 -> db상에서 모두 삭제 후 idnex.html로 이동한다. 실패 -> member/mypage.html
     * @author 민경
     */
    @Transactional
    @GetMapping("/memberDelete") //회원 탈퇴
    @ResponseBody
    public String memberdelete(@CurrentMember Member member) {

        List<Post> posts = postRepository.findByWriter(member);
        List<CommentCommunity> commentCommunityList = commentCommunityRepository.findByNickname(member.getNickname());
        List<CommentReview> commentReviews = commentReviewRepository.findByNickname(member.getNickname());
        List<Post> postdLike = postRepository.findBylikers(member);
        List<Post> postHit = postRepository.findByVisitedMember(member);

        if (commentReviews != null) { //리뷰 댓글 삭제
            for (int i = 0; i < commentReviews.size(); i++) {
                commentReviewRepository.delete(commentReviews.get(i));
            }
        }

        if (commentCommunityList != null) { //커뮤니티 댓글 삭제
            for (int i = 0; i < commentCommunityList.size(); i++) {
                commentCommunityRepository.delete(commentCommunityList.get(i));
            }
        }

        if (postdLike != null) { // 좋아요 삭제
            for (int i = 0; i < postdLike.size(); i++) {
                postdLike.get(i).setLikers(null);
                postRepository.save(postdLike.get(i));
            }
        }
        if (postHit != null) { //조회수 삭제
            for (int i = 0; i < postHit.size(); i++) {
                postHit.get(i).setVisitedMember(null);
                postRepository.save(postHit.get(i));
            }
        }

        if (posts != null) { //모든 게시물 삭제
            for (int i = 0; i < posts.size(); i++) {
                postRepository.delete(posts.get(i));
            }
        }
        memberRepository.delete(member);
        SecurityContextHolder.clearContext();
        JsonObject jsonObject = new JsonObject();

        return jsonObject.toString();
    }




    @RequestMapping("/idsearch")
    public String idSearchResult(String tel, Model model, String name) {
        log.info("tel : {}", tel);

        String message = null;
        try {

            if (name != null && tel != null) {

                Member member = memberService.findByName(name);
                if (member.getTel().equals(tel)) {
                    message = member.getUsername();
                } else {
                    message = "핸드폰 번호 명의가 이름과 일치하지 않습니다.";
                }
            }

        } catch (NoSuchElementException e) {
            message = "등록된 정보가 없습니다";
        }
        model.addAttribute("message", message);
        return "member/idsearch";
    }
}