// Static mock data. Persistence is connected in a later step.
export const previewData = {
  "schemaVersion": 8,
  "courses": [
    {
      "id": 1,
      "reservationId": null,
      "author": "여행러A",
      "mine": false,
      "title": "부산 바다와 맛집을 하루에",
      "region": "부산",
      "category": "맛집",
      "companion": "친구",
      "summary": "바다 구경도 하고 지역 맛집도 들렀던 일정이에요.",
      "body": "걸어서 이동한 구간이 많아서 편한 신발을 추천합니다. 해 질 무렵 해변이 좋았어요.",
      "photos": [
        "해운대.jpg",
        "돼지국밥.jpg"
      ],
      "plan": [
        {
          "day": 1,
          "stops": [
            "10:00 해운대 해수욕장",
            "12:00 식당 방문",
            "15:00 동백섬 산책"
          ]
        }
      ],
      "likes": 12,
      "likedByMe": false,
      "reportedByMe": false,
      "comments": [
        {
          "id": "c1",
          "author": "방문자1",
          "text": "걷는 시간이 어느 정도였나요?"
        },
        {
          "id": "c2",
          "author": "여행러A",
          "text": "대략 1시간 반 정도 걸었어요."
        }
      ],
      "createdAt": "2026-10-20",
      "visibility": "PUBLIC",
      "courseVersion": 1
    },
    {
      "id": 2,
      "reservationId": "R102",
      "author": "나(샘플 사용자)",
      "mine": true,
      "title": "강릉 커피와 바다 산책",
      "region": "강릉",
      "category": "힐링",
      "companion": "혼자",
      "summary": "해변과 카페를 천천히 둘러봤던 여행입니다.",
      "body": "안목해변에서 커피를 마시고, 다음 날에는 시장에 들렀어요.",
      "photos": [
        "카페거리.jpg"
      ],
      "plan": [
        {
          "day": 1,
          "stops": [
            "10:00 안목해변",
            "12:00 초당순두부 거리",
            "15:00 경포대"
          ]
        },
        {
          "day": 2,
          "stops": [
            "10:00 주문진 해변",
            "13:00 주문진 수산시장"
          ]
        }
      ],
      "likes": 4,
      "likedByMe": false,
      "reportedByMe": false,
      "comments": [
        {
          "id": "c3",
          "author": "방문자2",
          "text": "일정 참고할게요!"
        }
      ],
      "createdAt": "2026-10-18",
      "visibility": "PUBLIC",
      "courseVersion": 1
    },
    {
      "id": 3,
      "reservationId": null,
      "author": "산책좋아",
      "mine": false,
      "title": "서울 전시 + 골목 걷기 코스",
      "region": "서울",
      "category": "문화",
      "companion": "연인",
      "summary": "전시를 보고 주변 골목을 걷는 도심 코스입니다.",
      "body": "전시 관람 시간까지 여유롭게 잡는 것이 좋습니다.",
      "photos": [],
      "plan": [
        {
          "day": 1,
          "stops": [
            "11:00 전시 관람",
            "13:00 점심 식사",
            "15:00 북촌 산책"
          ]
        }
      ],
      "likes": 21,
      "likedByMe": false,
      "reportedByMe": false,
      "comments": [],
      "createdAt": "2026-10-17",
      "visibility": "PUBLIC",
      "courseVersion": 1
    },
    {
      "id": 4,
      "reservationId": null,
      "author": "도전파",
      "mine": false,
      "title": "제주 활동적인 1일 드라이브",
      "region": "제주",
      "category": "액티비티",
      "companion": "가족",
      "summary": "전망대와 산책 코스를 묶은 제주 하루 코스입니다.",
      "body": "자동차 이동을 가정한 일정으로, 도로 상황에 따라 소요시간이 달라져요.",
      "photos": [
        "전망대.jpg"
      ],
      "plan": [
        {
          "day": 1,
          "stops": [
            "09:30 해안 전망대",
            "12:30 점심",
            "14:00 숲길 걷기"
          ]
        }
      ],
      "likes": 8,
      "likedByMe": false,
      "reportedByMe": false,
      "comments": [],
      "createdAt": "2026-10-16",
      "visibility": "PUBLIC",
      "courseVersion": 1
    },
    {
      "id": 5,
      "reservationId": null,
      "author": "여행러A",
      "mine": false,
      "title": "관리자 블라인드 상태 예시",
      "region": "부산",
      "category": "맛집",
      "companion": "친구",
      "summary": "바다 구경도 하고 지역 맛집도 들렀던 일정이에요.",
      "body": "걸어서 이동한 구간이 많아서 편한 신발을 추천합니다. 해 질 무렵 해변이 좋았어요.",
      "photos": [
        "해운대.jpg",
        "돼지국밥.jpg"
      ],
      "plan": [
        {
          "day": 1,
          "stops": [
            "10:00 해운대 해수욕장",
            "12:00 식당 방문",
            "15:00 동백섬 산책"
          ]
        }
      ],
      "likes": 12,
      "likedByMe": false,
      "reportedByMe": true,
      "comments": [
        {
          "id": "c1",
          "author": "방문자1",
          "text": "걷는 시간이 어느 정도였나요?"
        },
        {
          "id": "c2",
          "author": "여행러A",
          "text": "대략 1시간 반 정도 걸었어요."
        }
      ],
      "createdAt": "2026-10-20",
      "visibility": "BLINDED",
      "courseVersion": 1,
      "moderationReason": "신고 검토 후 부적절한 게시글로 판단한 샘플"
    }
  ],
  "reservations": [
    {
      "id": "R101",
      "name": "부산 해운대 숙소",
      "region": "부산",
      "dates": "2026-10-12 ~ 2026-10-14",
      "status": "COMPLETED",
      "itineraryFinal": true,
      "plan": [
        {
          "day": 1,
          "stops": [
            "11:00 해운대 해변",
            "12:30 돼지국밥 식당",
            "15:00 블루라인파크"
          ]
        },
        {
          "day": 2,
          "stops": [
            "10:00 흰여울문화마을",
            "12:30 남포동 점심",
            "15:00 감천문화마을"
          ]
        }
      ],
      "checkoutAt": "2026-10-14T11:00:00+09:00",
      "courseVersion": 3,
      "versions": [
        {
          "version": 1,
          "plan": [
            {
              "day": 1,
              "stops": [
                "11:00 해운대 해변",
                "15:00 초기 코스"
              ]
            }
          ]
        },
        {
          "version": 3,
          "plan": [
            {
              "day": 1,
              "stops": [
                "11:00 해운대 해변",
                "12:30 돼지국밥 식당",
                "15:00 블루라인파크"
              ]
            },
            {
              "day": 2,
              "stops": [
                "10:00 흰여울문화마을",
                "12:30 남포동 점심",
                "15:00 감천문화마을"
              ]
            }
          ]
        }
      ]
    },
    {
      "id": "R102",
      "name": "강릉 중앙 숙소",
      "region": "강릉",
      "dates": "2026-09-03 ~ 2026-09-05",
      "status": "COMPLETED",
      "itineraryFinal": true,
      "plan": [
        {
          "day": 1,
          "stops": [
            "10:00 안목해변",
            "12:00 초당순두부 거리",
            "15:00 경포대"
          ]
        },
        {
          "day": 2,
          "stops": [
            "10:00 주문진 해변",
            "13:00 주문진 수산시장"
          ]
        }
      ],
      "checkoutAt": "2026-09-05T11:00:00+09:00",
      "courseVersion": 1
    },
    {
      "id": "R103",
      "name": "제주 서귀포 숙소",
      "region": "제주",
      "dates": "2026-08-11 ~ 2026-08-13",
      "status": "CANCELED",
      "itineraryFinal": true,
      "plan": [
        {
          "day": 1,
          "stops": [
            "10:00 천지연폭포",
            "14:00 올레길"
          ]
        }
      ],
      "checkoutAt": "2026-08-13T11:00:00+09:00",
      "courseVersion": 1
    },
    {
      "id": "R104",
      "name": "경주 시내 숙소",
      "region": "경주",
      "dates": "2026-11-18 ~ 2026-11-19",
      "status": "UPCOMING",
      "itineraryFinal": true,
      "plan": [
        {
          "day": 1,
          "stops": [
            "11:00 대릉원",
            "14:00 황리단길"
          ]
        }
      ],
      "checkoutAt": "2026-11-19T11:00:00+09:00",
      "courseVersion": 1
    },
    {
      "id": "R105",
      "name": "서울 종로 숙소",
      "region": "서울",
      "dates": "2026-09-25 ~ 2026-09-26",
      "status": "COMPLETED",
      "itineraryFinal": false,
      "plan": [],
      "checkoutAt": "2026-09-26T11:00:00+09:00",
      "courseVersion": 1
    },
    {
      "id": "R106",
      "name": "부산 배치 전환 예시",
      "region": "부산",
      "dates": "2026-10-25 ~ 2026-10-26",
      "status": "CONFIRMED",
      "checkoutAt": "2026-10-26T11:00:00+09:00",
      "itineraryFinal": true,
      "courseVersion": 2,
      "plan": [
        {
          "day": 1,
          "stops": [
            "13:00 부산시민공원",
            "16:00 전포 카페거리"
          ]
        }
      ]
    },
    {
      "id": "R107",
      "name": "부산 체크아웃 전 예시",
      "region": "부산",
      "dates": "2026-10-25 ~ 2026-10-26",
      "status": "CONFIRMED",
      "checkoutAt": "2026-10-26T12:30:00+09:00",
      "itineraryFinal": true,
      "courseVersion": 1,
      "plan": [
        {
          "day": 1,
          "stops": [
            "14:00 광안리 해변"
          ]
        }
      ]
    }
  ],
  "nextId": 6
};
export const previewCourses = previewData.courses;
export const previewReservations = previewData.reservations;
