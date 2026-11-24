package com.smarttraveler.tour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.stay.dto.StayDetailResponse.Photo;
import com.smarttraveler.tour.client.TourApiClient;
import com.smarttraveler.tour.client.TourApiClient.Page;
import com.smarttraveler.tour.client.TourApiClient.TourApiException;
import com.smarttraveler.tour.service.TourPhotoService;
import com.smarttraveler.tour.service.TourPhotoService.Target;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class TourPhotoServiceTest {
    private final TourApiClient api = mock(TourApiClient.class);
    private final TourPhotoService service = new TourPhotoService(api);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Page empty = new Page(0, List.of());

    @Test
    void usesKnownContentIdAndReturnsAlternatesWithoutRepeatingLookup() {
        var target = new Target("32", "예약한 숙소", "서울 중구", "", true);
        when(api.photoDetails("32")).thenReturn(new Page(1, List.of(mapper.readTree("""
                {"originimgurl":"http://photos.example/original.jpg","smallimageurl":"https://photos.example/thumb.jpg"}
                """))));
        when(api.photoCover("32")).thenReturn(new Page(1, List.of(mapper.readTree("""
                {"firstimage":"https://photos.example/original.jpg","firstimage2":"https://photos.example/alternate.jpg"}
                """))));
        var expected = List.of("https://photos.example/original.jpg", "https://photos.example/thumb.jpg",
                "https://photos.example/alternate.jpg");
        assertEquals(expected, service.photos(target));
        assertEquals(expected, service.photos(target));
        verify(api).photoDetails("32");
        verify(api, never()).photoCandidates("예약한 숙소", true);
    }

    @Test
    void usesMatchingAddressAndSkipsTheSameNameInAnotherRegion() {
        var target = new Target(null, "박달재수련원", "충북 제천시 봉양읍 박달로 256-46", "제천", false);
        when(api.photoCandidates("박달재수련원", false)).thenReturn(new Page(2, List.of(
                mapper.readTree("""
                        {"title":"박달재수련원","addr1":"충청북도 제천시 봉양읍 박달로 256-46","contentid":"11",
                         "firstimage":"https://photos.example/correct.jpg"}
                        """),
                mapper.readTree("""
                        {"title":"박달재수련원","addr1":"충청남도 공주시 다른로 12","contentid":"22",
                         "firstimage":"https://photos.example/wrong.jpg"}
                        """))));
        when(api.photoDetails("11")).thenReturn(empty);
        assertEquals(List.of("https://photos.example/correct.jpg"), service.photos(target));
        verify(api, never()).photoDetails("22");
    }

    @Test
    void reportsOutageSeparatelyFromAConfirmedEmptyResult() {
        var target = new Target("11", "장소", "", "", false);
        when(api.photoDetails("11")).thenThrow(new TourApiException("timeout")).thenReturn(empty);
        when(api.photoCover("11")).thenReturn(empty);
        var error = assertThrows(ResponseStatusException.class, () -> service.photos(target));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertEquals(List.of(), service.photos(target));
    }

    @Test
    void loadsAdditionalLodgingPagesAndKeepsRoomPhotosGroupedByProviderRoomTitle() {
        var lodging = new Lodging();
        lodging.setSourceProvider("TOURAPI");
        lodging.setSourceContentId("92");
        when(api.photoDetails("92", 1)).thenReturn(new Page(2, List.of(mapper.readTree("""
                {"originimgurl":"https://photos.example/lobby.jpg","smallimageurl":"https://photos.example/lobby-small.jpg"}
                """))));
        when(api.photoDetails("92", 2)).thenReturn(new Page(2, List.of(mapper.readTree("""
                {"originimgurl":"https://photos.example/exterior.jpg"}
                """))));
        when(api.photoCover("92")).thenReturn(new Page(1, List.of(mapper.readTree("""
                {"firstimage":"https://photos.example/cover.jpg"}
                """))));
        when(api.roomPhotos("92", 1)).thenReturn(new Page(1, List.of(mapper.readTree("""
                {"roomtitle":"더블룸","roomimg1":"https://photos.example/double.jpg"}
                """))));
        var gallery = service.lodgingGallery(lodging);
        assertEquals(List.of(new Photo("https://photos.example/cover.jpg", null),
                new Photo("https://photos.example/lobby.jpg", "https://photos.example/lobby-small.jpg"),
                new Photo("https://photos.example/exterior.jpg", null)), gallery.photos());
        assertEquals(List.of(new TourPhotoService.RoomPhotos("더블룸", List.of(new Photo("https://photos.example/double.jpg", null)))),
                gallery.rooms());
    }
}
