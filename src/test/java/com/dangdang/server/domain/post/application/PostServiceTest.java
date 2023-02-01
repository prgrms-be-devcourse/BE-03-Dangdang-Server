package com.dangdang.server.domain.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.post.domain.Category;
import com.dangdang.server.domain.post.domain.PostRepository;
import com.dangdang.server.domain.post.dto.request.PostSaveRequest;
import com.dangdang.server.domain.post.dto.request.PostUpdateRequest;
import com.dangdang.server.domain.post.dto.response.PostDetailResponse;
import com.dangdang.server.domain.post.dto.response.PostResponse;
import com.dangdang.server.domain.post.exception.PostNotFoundException;
import com.dangdang.server.domain.postImage.domain.PostImageRepository;
import com.dangdang.server.domain.postImage.domain.entity.PostImage;
import com.dangdang.server.domain.postImage.dto.PostImageRequest;
import com.dangdang.server.domain.town.domain.TownRepository;
import com.dangdang.server.domain.town.domain.entity.Town;
import com.dangdang.server.global.exception.UrlInValidException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostServiceTest {

  @Autowired
  PostService postService;
  @Autowired
  MemberRepository memberRepository;
  @Autowired
  TownRepository townRepository;
  @Autowired
  PostImageRepository postImageRepository;

  @Value("${s3.bucket}")
  String bucketName;
  @Value("${cloud.aws.region.static}")
  private String region;
  @Autowired
  private PostRepository postRepository;

  @Test
  @DisplayName("게시글을 작성할 수 있다.")
  void postSaveTest() {
    Member loginMember = new Member("테스트 멤버", "01012341234", "testImgUrl");
    memberRepository.save(loginMember);
    Town town = new Town("서현동", null, null);
    townRepository.save(town);

    PostImageRequest postImageRequest = new PostImageRequest(
        List.of("http://s3.amazonaws.com/test1.png", "http://s3.amazonaws.com/test2.png"));

    PostSaveRequest postSaveRequest = new PostSaveRequest("title1", "content1", Category.디지털기기,
        1000, "서현동 코지카페", BigDecimal.valueOf(123L), BigDecimal.valueOf(123L), false, "서현동",
        postImageRequest);
    PostDetailResponse savedPostResponse = postService.savePost(postSaveRequest, loginMember);

    PostDetailResponse foundPost = postService.findPostDetailById(savedPostResponse.postId());
    assertThat(savedPostResponse.getPostResponse()).usingRecursiveComparison()
        .isEqualTo(foundPost.getPostResponse());
    assertThat(savedPostResponse.getMemberResponse()).usingRecursiveComparison()
        .isEqualTo(foundPost.getMemberResponse());
  }

  @Test
  @DisplayName("게시글 조회시 해당하는 id값이 없다면 PostNotFoundException이 발생한다.")
  void findPostByIdInCorrect() {
    Long wrongId = 9999L;

    assertThatThrownBy(() -> postService.findPostDetailById(wrongId)).isInstanceOf(
        PostNotFoundException.class);
  }

  @Test
  @DisplayName("게시글을 상세 조회 할 수 있다.")
  void findPostDetailById() {
    Member loginMember = new Member("테스트 멤버", "01012341234", "testImgUrl");
    memberRepository.save(loginMember);
    Town town = new Town("서현동", null, null);
    townRepository.save(town);

    PostImageRequest postImageRequest = new PostImageRequest(
        Arrays.asList("http://s3.amazonaws.com/test1.png", "http://s3.amazonaws.com/test2.png"));

    PostSaveRequest postSaveRequest = new PostSaveRequest("title1", "content1", Category.디지털기기,
        1000, "서현동 코지카페", BigDecimal.valueOf(123L), BigDecimal.valueOf(123L), false, "서현동",
        postImageRequest);

    PostDetailResponse savedPostResponse = postService.savePost(postSaveRequest, loginMember);

    PostDetailResponse foundPost = postService.findPostDetailById(savedPostResponse.postId());
    assertThat(foundPost).isNotNull();
    assertThat(foundPost.getImageUrls()).hasSize(2);
  }

  @Test
  @DisplayName("게시글을 상세 조회 시 열어볼 수 있는 이미지 링크를 제공할 수 있다.")
  void findPostDetailByIdOpenImageLink() {
    Member loginMember = new Member("테스트 멤버", "01012341234", "testImgUrl");
    memberRepository.save(loginMember);
    Town town = new Town("서현동", null, null);
    townRepository.save(town);

    PostImageRequest postImageRequest = new PostImageRequest(Arrays.asList(
        "https://" + bucketName + ".s3." + region + ".amazonaws.com/post-image/test2.png",
        "https://" + bucketName + ".s3." + region + ".amazonaws.com/post-image/test3.png"));

    PostSaveRequest postSaveRequest = new PostSaveRequest("title1", "content1", Category.디지털기기,
        1000, "서현동 코지카페", BigDecimal.valueOf(123L), BigDecimal.valueOf(123L), false, "서현동",
        postImageRequest);

    PostDetailResponse savedPostResponse = postService.savePost(postSaveRequest, loginMember);

    PostDetailResponse foundPost = postService.findPostDetailById(savedPostResponse.postId());
    Assertions.assertThat(foundPost.getImageUrls()).hasSize(2);
  }

  @Test
  @DisplayName("게시글 1개 조회 시 이미지 URL이 잘못되면 UrlInvalidException이 발생한다.")
  void findPostDetailByIdThrowUrlInvalidException() {
    Member loginMember = new Member("테스트 멤버", "01012341234", "testImgUrl");
    memberRepository.save(loginMember);
    Town town = new Town("서현동", null, null);
    townRepository.save(town);

    PostImageRequest wrongPostImageRequest = new PostImageRequest(Arrays.asList("url1", "url2"));

    PostSaveRequest postSaveRequest = new PostSaveRequest("title1", "content1", Category.디지털기기,
        1000, "서현동 코지카페", BigDecimal.valueOf(123L), BigDecimal.valueOf(123L), false, "서현동",
        wrongPostImageRequest);

    PostDetailResponse savedPostResponse = postService.savePost(postSaveRequest, loginMember);

    assertThatThrownBy(
        () -> postService.findPostDetailById(savedPostResponse.postId()))
        .isInstanceOf(UrlInValidException.class);
  }

  @Test
  @DisplayName("게시글을 수정할 수 있다.")
  void updatePostTest() {
    //given
    Member newMember = new Member("테스트 멤버", "01012341234", "testImgUrl");
    Member member = memberRepository.save(newMember);

    PostImageRequest postImageRequest = new PostImageRequest(
        Arrays.asList("http://s3.amazonaws.com/test1.png", "http://s3.amazonaws.com/test2.png"));

    PostSaveRequest postSaveRequest = new PostSaveRequest("title1", "content1", Category.디지털기기,
        1000, "천호동 코지카페", BigDecimal.valueOf(123L), BigDecimal.valueOf(123L), false, "천호동",
        postImageRequest);

    PostDetailResponse savedPostResponse = postService.savePost(postSaveRequest, member);
    PostResponse postResponse = savedPostResponse.getPostResponse();

    String updateTitle = "updateTitle";
    String updateContent = "updateContent";
    Integer updatePrice = 2000;
    Category updateCategory = Category.생활가전;
    String updateDesiredPlaceName = "길거리";
    BigDecimal updateLongitude = BigDecimal.valueOf(127.0000);
    BigDecimal updateLatitude = BigDecimal.valueOf(36.0000);
    String updateUrlOne = "http://s3.amazonaws.com/updateTest1";
    String updateUrlTwo = "http://s3.amazonaws.com/updateTest2";
    PostImageRequest updatePostImageRequest = new PostImageRequest(
        Arrays.asList(updateUrlOne, updateUrlTwo));

    //when
    PostUpdateRequest postUpdateRequest = new PostUpdateRequest(postResponse.getId(),
        updateTitle, updateContent,
        updateCategory, updatePrice,
        updateDesiredPlaceName,
        updateLongitude,
        updateLatitude, false, updatePostImageRequest);

    PostDetailResponse postDetailResponse = postService.updatePost(postUpdateRequest, member);

    //then
    PostDetailResponse resultPostDetail = postService.findPostDetailById(postResponse.getId());

    assertThat(postDetailResponse.getPostResponse()).usingRecursiveComparison()
        .isEqualTo(resultPostDetail.getPostResponse());
    List<String> imageUrls = postImageRepository.findPostImagesByPostId(postResponse.getId())
        .stream().map(
            PostImage::getUrl).collect(Collectors.toList());
    Assertions.assertThat(imageUrls).contains(updateUrlOne, updateUrlTwo);
  }
}