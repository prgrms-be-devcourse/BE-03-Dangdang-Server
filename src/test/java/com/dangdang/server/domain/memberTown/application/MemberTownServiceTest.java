package com.dangdang.server.domain.memberTown.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dangdang.server.domain.common.StatusType;
import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.memberTown.domain.MemberTownRepository;
import com.dangdang.server.domain.memberTown.domain.entity.MemberTown;
import com.dangdang.server.domain.memberTown.domain.entity.RangeType;
import com.dangdang.server.domain.memberTown.dto.request.MemberTownRangeRequest;
import com.dangdang.server.domain.memberTown.dto.request.MemberTownRequest;
import com.dangdang.server.domain.memberTown.exception.MemberTownNotFoundException;
import com.dangdang.server.domain.memberTown.exception.NotAppropriateCountException;
import com.dangdang.server.domain.memberTown.exception.NotAppropriateRangeException;
import com.dangdang.server.domain.town.domain.TownRepository;
import com.dangdang.server.domain.town.domain.entity.Town;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class MemberTownServiceTest {

  @Autowired
  MemberRepository memberRepository;

  @Autowired
  TownRepository townRepository;

  @Autowired
  MemberTownRepository memberTownRepository;

  @Autowired
  MemberTownService memberTownService;

  Member member;

  @BeforeEach
  void setup() {
    member = new Member("01012345678", null, "Albatross");
    memberRepository.save(member);
  }

  @AfterEach
  void clear() {
    memberRepository.deleteById(member.getId());
  }

  @Test
  @DisplayName("멤버 타운이 생성 성공")
  @Transactional
  void createMemberTown() {
    // given
    // member-town 1개 생성되어 있어야 (가입할 때 되어 있는 부분)
    Town existingTown = townRepository.findByName("삼성2동").get();
    MemberTown existingMemberTown = new MemberTown(member, existingTown);
    memberTownRepository.save(existingMemberTown);

    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성1동");

    // when
    memberTownService.createMemberTown(memberTownRequest,
        member);

    // then
    // memberTown list size 가 2개인지 확인, 상태가 변경되었는지 확인
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(member.getId());
    assertThat(memberTownList.size()).isEqualTo(2);

    assertThat(memberTownList.get(0).getStatus()).isEqualTo(StatusType.INACTIVE);
    assertThat(memberTownList.get(1).getStatus()).isEqualTo(StatusType.ACTIVE);

  }

  @Test
  @DisplayName("멤버 타운 생성 실패 - 멤버 타운 개수가 1이 아닌 경우")
  void createMemberTown_fail() {
    // given
    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성1동");

    // when, then
    assertThatThrownBy(() -> memberTownService.createMemberTown(memberTownRequest, member))
        .isInstanceOf(NotAppropriateCountException.class);
  }

  @Test
  @DisplayName("멤버 타운 삭제 성공- Inactive 삭제한 경우")
  @Transactional
  void deleteMemberTown_withInactive() {
    // given
    // member-town 2개 있어야 함 (existingMemberTown1이 Inactive 가 된다)
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    Town existingTown2 = townRepository.findByName("삼성2동").get();
    MemberTown existingMemberTown2 = new MemberTown(member, existingTown2);
    memberTownRepository.save(existingMemberTown2);

    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성1동");

    // when
    memberTownService.deleteMemberTown(memberTownRequest, member);

    // then
    // size 는 1이고 삼성 2동이며 Active 상태
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(member.getId());
    assertThat(memberTownList.size()).isEqualTo(1);
    assertThat(memberTownList.get(0).getTown().getName()).isEqualTo("삼성2동");
    assertThat(memberTownList.get(0).getStatus()).isEqualTo(StatusType.ACTIVE);
  }

  @Test
  @DisplayName("멤버 타운이 삭제 성공 - Active 삭제한 경우 ")
  @Transactional
  void deleteMemberTown_withActive() {
    // given
    // member-town 2개 있어야 함 (existingMemberTown1이 Inactive 가 된다)
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    Town existingTown2 = townRepository.findByName("삼성2동").get();
    MemberTown existingMemberTown2 = new MemberTown(member, existingTown2);
    memberTownRepository.save(existingMemberTown2);

    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성2동");

    // when
    memberTownService.deleteMemberTown(memberTownRequest, member);

    // then
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(member.getId());
    assertThat(memberTownList.size()).isEqualTo(1);
    assertThat(memberTownList.get(0).getTown().getName()).isEqualTo("삼성1동");
    assertThat(memberTownList.get(0).getStatus()).isEqualTo(StatusType.ACTIVE);
  }

  @Test
  @DisplayName("멤버 타운 삭제 실패 - 멤버 타운 개수가 2개가 아닌 경우")
  void deleteMemberTown_withMemberTownNotSize2() {
    // when
    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성1동");

    // then, given
    assertThatThrownBy(() -> memberTownService.deleteMemberTown(memberTownRequest, member))
        .isInstanceOf(NotAppropriateCountException.class);
  }

  @Test
  @DisplayName("멤버 타운 삭제 실패 - 잘못된 멤버타운 이름이 넘겨진 경우")
  @Transactional
  void deleteMemberTown_withWrongMemberTownName() {
    // given
    // member-town 2개 있어야 함 (existingMemberTown1이 Inactive 가 된다)
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    Town existingTown2 = townRepository.findByName("삼성2동").get();
    MemberTown existingMemberTown2 = new MemberTown(member, existingTown2);
    memberTownRepository.save(existingMemberTown2);

    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성7동");

    // then, given
    assertThatThrownBy(() -> memberTownService.deleteMemberTown(memberTownRequest, member))
        .isInstanceOf(MemberTownNotFoundException.class);
  }

  @Test
  @DisplayName("멤버 타운 활성화 변경 성공")
  @Transactional
  void changeActiveMemberTown() {
    // given
    // member-town 2개 있어야 함 (삼성1동이 Inactive 가 된다)
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    Town existingTown2 = townRepository.findByName("삼성2동").get();
    MemberTown existingMemberTown2 = new MemberTown(member, existingTown2);
    memberTownRepository.save(existingMemberTown2);

    // 삼성 1동으로 Active 변경 요청
    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성1동");

    // when
    memberTownService.changeActiveMemberTown(memberTownRequest, member);

    // then
    // 삼성 1동 Active, 삼성 2동 Inactive
    List<MemberTown> memberTownList = memberTownRepository.findByMemberId(member.getId());
    assertThat(memberTownList.get(0).getStatus()).isEqualTo(StatusType.ACTIVE);
    assertThat(memberTownList.get(1).getStatus()).isEqualTo(StatusType.INACTIVE);
  }

  @Test
  @DisplayName("멤버 타운 활성화 변경 실패 - 멤버 타운 개수가 2개가 아닌 경우")
  @Transactional
  void changeActiveMemberTown_withMemberTownNotSize2() {
    // given
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    MemberTownRequest memberTownRequest = new MemberTownRequest("삼성2동");

    // when, then
    assertThatThrownBy(() -> memberTownService.changeActiveMemberTown(memberTownRequest, member))
        .isInstanceOf(NotAppropriateCountException.class);
  }

  @Test
  @DisplayName("멤버 타운 range 변경 성공")
  @Transactional
  void changeMemberTownRange() {
    // given
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    MemberTownRangeRequest memberTownRangeRequest = new MemberTownRangeRequest("삼성1동", 3);

    // when
    memberTownService.changeMemberTownRange(memberTownRangeRequest, member);

    // then
    // range 가 변경 되었는지 확인
    Town foundTown = townRepository.findByName("삼성1동").get();
    MemberTown foundMemberTown = memberTownRepository.findByMemberIdAndTownId(
        member.getId(), foundTown.getId()).get();

    assertThat(foundMemberTown.getRangeType()).isEqualTo(RangeType.LEVEL3);
  }

  @Test
  @DisplayName("멤버 타운 range 변경 실패 - 멤버 타운 없는 경우")
  @Transactional
  void changeMemberTownRange_withWrongMemberTown() {
    // given
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    MemberTownRangeRequest memberTownRangeRequest = new MemberTownRangeRequest("삼성2동", 3);

    // when, then
    assertThatThrownBy(
        () -> memberTownService.changeMemberTownRange(memberTownRangeRequest, member))
        .isInstanceOf(MemberTownNotFoundException.class);
  }
  // 실패 2 - range 범위가 잘못된 경우
  @Test
  @DisplayName("멤버 타운 range 변경 실패 - range 범위가 잘못된 경우")
  @Transactional
  void changeMemberTownRange_withWrongMemberTownRange() {
    // given
    Town existingTown1 = townRepository.findByName("삼성1동").get();
    MemberTown existingMemberTown1 = new MemberTown(member, existingTown1);
    memberTownRepository.save(existingMemberTown1);

    MemberTownRangeRequest memberTownRangeRequest = new MemberTownRangeRequest("삼성1동", 7);

    // when, then
    assertThatThrownBy(
        () -> memberTownService.changeMemberTownRange(memberTownRangeRequest, member))
        .isInstanceOf(NotAppropriateRangeException.class);
  }
}