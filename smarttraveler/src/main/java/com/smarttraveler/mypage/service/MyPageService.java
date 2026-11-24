package com.smarttraveler.mypage.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.mypage.dto.MyPageDtos.*;
import com.smarttraveler.mypage.repository.MyPageRepository;
import com.smarttraveler.tour.service.TourPhotoService.Target;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MyPageService {
    private final MemberRepository members;
    private final MyPageRepository data;
    public MyPageService(MemberRepository members, MyPageRepository data) { this.members = members; this.data = data; }

    public long owner(Authentication auth, Long expectedMemberId) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) throw denied(HttpStatus.UNAUTHORIZED);
        var member = members.findByLoginId(auth.getName()).orElseThrow(() -> denied(HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equals(member.getStatus()) || member.getDeletedAt() != null) throw denied(HttpStatus.UNAUTHORIZED);
        if (expectedMemberId != null && !expectedMemberId.equals(member.getMemberId())) throw denied(HttpStatus.FORBIDDEN);
        return member.getMemberId();
    }

    public Snapshot snapshot(Authentication auth, Long expectedMemberId) {
        long id = owner(auth, expectedMemberId);
        return new Snapshot(id, data.courses(id), data.bookings(id), data.posts(id));
    }

    public CourseView course(Authentication auth, Long expectedMemberId, String courseId) {
        long id = owner(auth, expectedMemberId);
        if (!courseId.matches("(draft|course)-[1-9][0-9]{0,17}")) throw denied(HttpStatus.NOT_FOUND);
        var course = data.course(id, courseId);
        // 없는 자료와 타인 자료를 구분해 노출하지 않는다.
        if (course == null) throw denied(HttpStatus.NOT_FOUND);
        return course;
    }

    public PlaceDetails placeDetails(Authentication auth, Long placeId, String name, String region) {
        owner(auth, null);
        validatePlace(placeId, name, region);
        var place = data.placeDetails(placeId,name.trim(),region.trim());
        if (place==null) throw denied(HttpStatus.NOT_FOUND);
        return place;
    }

    public Target placePhotoTarget(Authentication auth, Long placeId, String name, String address, String region) {
        owner(auth, null);
        validatePlace(placeId, name, region);
        if (address.length() > 250) throw denied(HttpStatus.BAD_REQUEST);
        if (placeId != null) {
            var target = data.placePhotoTarget(placeId, address.trim(), region.trim());
            if (target != null) return target;
        }
        return new Target(null, name.trim(), address.trim(), region.trim(), false);
    }

    public Target lodgingPhotoTarget(Authentication auth, String bookingId) {
        var booking = booking(auth, null, bookingId);
        Long lodgingId = booking.stayInfo() != null ? booking.stayInfo().id()
                : booking.itinerary() != null && booking.itinerary().stay() != null ? booking.itinerary().stay().lodgingId() : null;
        return lodgingId == null ? null : data.lodgingPhotoTarget(lodgingId);
    }

    private void validatePlace(Long placeId, String name, String region) {
        if ((placeId != null && placeId <= 0) || name.length() > 200 || region.length() > 100
                || (placeId == null && name.isBlank())) throw denied(HttpStatus.BAD_REQUEST);
    }

    public BookingView booking(Authentication auth, Long expectedMemberId, String bookingId) {
        long id = owner(auth, expectedMemberId);
        return data.bookings(id).stream().filter(b -> b.id().equals(bookingId)).findFirst()
                .orElseThrow(() -> denied(HttpStatus.NOT_FOUND));
    }

    public PostView post(Authentication auth, Long expectedMemberId, String postId) {
        long id = owner(auth, expectedMemberId);
        return data.posts(id).stream().filter(p -> p.id().equals(postId)).findFirst()
                .orElseThrow(() -> denied(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ImportResult importCourses(Authentication auth, ImportRequest request) {
        long id = owner(auth, request.expectedMemberId());
        var clientIds = new HashSet<String>();
        for (var draft : request.courses()) {
            long length = ChronoUnit.DAYS.between(draft.startDate(), draft.endDate()) + 1;
            if (length < 1 || length > 31 || draft.days().size() != length || !clientIds.add(draft.clientId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 기간과 중복 코스를 확인해 주세요.");
            }
            for (int day = 0; day < draft.days().size(); day++) {
                if (!draft.days().get(day).date().equals(draft.startDate().plusDays(day))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 날짜는 여행 기간의 순서와 같아야 합니다.");
                }
            }
        }
        if (!data.lockMember(id)) throw denied(HttpStatus.UNAUTHORIZED);
        request.courses().forEach(draft -> data.importDraft(id, draft));
        return new ImportResult(request.courses().stream().map(Draft::clientId).toList());
    }

    private ResponseStatusException denied(HttpStatus status) {
        return new ResponseStatusException(status, status == HttpStatus.UNAUTHORIZED ? "로그인이 필요합니다." : "접근할 수 없는 자료입니다.");
    }
}
