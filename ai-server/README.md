# 스마트 트래블 Python AI 서버

Python 서버는 LangChain의 `ChatOpenAI`로 대화에서 조건을 추출하고, LangGraph로 조건 검증과 다음 질문을 처리한다. 대화·조건 저장은 기존 Spring Boot와 MariaDB가 담당한다. 한국관광공사의 지역·관광지 API를 조회하고 Spring 서버가 선택한 지역과 조회한 관광지를 DB에 저장한다. 대화에서 사용자가 확인하거나 추천을 요청하면 Spring이 전달한 저장 관광지 후보 중 최대 세 곳을 추천한다.

ai-server는 MariaDB의 장소 데이터를 직접 읽어 ChromaDB(`text-embedding-3-small`)에 넣고, 검색된 장소를 근거로 LangGraph 코스 그래프(검색→조합→OpenAI 응답 생성→검증)가 코스를 만드는 RAG 코스 추천 기능도 갖고 있다(아래 "MariaDB → ChromaDB 적재와 코스 추천" 참고). 다만 Spring의 최종 사용자용 코스 추천 API(`/api/courses/recommendations`)는 아직 이 기능에 연결되지 않아 503을 반환한다 — 지금은 ai-server 내부 API(`/internal/courses/recommend`)로만 확인할 수 있다.

## 실행 방법 (Windows PowerShell)

아래 명령은 `ai-server` 폴더에서 실행한다. API 키는 이 Python 서버를 실행하는 터미널에서 설정해야 한다.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:OPENAI_API_KEY = Read-Host "OpenAI API 키를 입력하세요"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

관광지 조회를 사용하려면 Python 서버를 실행하기 전에 같은 터미널에서 `$env:TOUR_API_KEY = Read-Host "공공데이터 일반 인증키(Decoding)"`를 실행해 한국관광공사 국문 관광정보 서비스용 일반 인증키를 설정한다. 이미 키가 현재 터미널에 설정되어 있다면 다시 입력하지 않아도 된다. PowerShell 창을 닫으면 해당 창에만 설정한 환경변수는 사라진다. 키를 코드, 문서, Git에 기록하거나 공유하지 않는다. 기본 모델은 `gpt-4o-mini`이고 필요하면 Python 서버 실행 전에 `OPENAI_MODEL` 환경변수로 변경할 수 있다.

별도 터미널에서 기존 `smarttraveler`의 `./gradlew.bat bootRun`과 `frontend`의 `npm run dev`를 실행한다. MariaDB는 기존 대화 저장을 위해 필요하다. Spring 서버의 AI 서버 주소 기본값은 `http://127.0.0.1:8001`이며 다른 주소가 필요하면 Spring 서버를 실행하는 환경에서 `AI_SERVICE_URL`을 지정한다.

`GET http://127.0.0.1:8001/health`는 Python 서버의 실행 여부만 보여준다. API 키나 OpenAI 호출의 성공 여부를 확인하는 엔드포인트는 아니다.

`POST /internal/dialogue`는 Spring 서버가 내부적으로 호출한다. 로컬 개발 환경 기준이므로 Python 서버를 `127.0.0.1`에만 바인딩한다. 외부 배포 시 서비스 간 인증·암호화와 접근 제한을 별도로 구성해야 한다.

## MariaDB → ChromaDB 적재와 코스 추천

ai-server는 Spring과 같은 MariaDB(`smart_travel_db`)를 **읽기만** 한다. `.env`에 `OPENAI_API_KEY` 아래로 접속 정보를 추가한다(비밀번호는 Git에 올리지 않는다, 가능하면 SELECT 권한만 있는 계정 사용을 권장).

```
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=smart_travel_db
DB_USERNAME=(Spring과 같은 계정 또는 읽기 전용 계정)
DB_PASSWORD=(비밀번호)
```

지역의 장소를 MariaDB에서 읽어 Chroma에 넣으려면(처음 실행, `chroma_data` 복구, 관리자가 DB에서 장소를 수정·비활성화했을 때) 아래 명령을 쓴다. 문서가 바뀐 장소만 다시 임베딩하므로 OpenAI 비용이 절약되고, MariaDB에서 비활성화된 장소는 Chroma에서 함께 지워진다.

```powershell
cd D:\sjtool\workspace\team-project3\travle\smart-traveler\ai-server
.\.venv\Scripts\python.exe -m scripts.sync_mariadb_to_chroma 26350
# 예: 26350 {'read': 208, 'skipped': 0, 'embedded': 0, 'unchanged': 208, 'removed': 0}
```

서버가 떠 있을 때는 `POST /internal/places/sync`로도 같은 적재를 호출할 수 있다(`{"sigunguCode": "26350"}`).

적재가 끝나면 아래 두 API로 코스를 만들 수 있다. 어떤 장소를 쓸지는 Chroma 검색 점수와 코드가 정하고, OpenAI(LangChain `ChatOpenAI`)는 그 결과를 문장으로만 다듬는다 — OpenAI가 실패해도 코스 자체(장소·순서·시간)는 똑같이 나오고 문장만 초안으로 바뀐다(`polished: false`).

| API | 설명 |
| --- | --- |
| `POST /internal/places/search` | 앵커 1곳(이름으로 지정 또는 자동 선택) + 슬롯별(MEAL/FUN/MARKET) 후보 Top-5. 확인용 |
| `POST /internal/courses/recommend` | LangGraph 코스 그래프 실행: Chroma 검색(RAG) → 조합 → OpenAI 응답 생성 → 검증. 앵커나 식당 후보가 없으면 422 |

```powershell
$body = @{ sigunguCode = "26350"; anchorName = "해운대"; transportType = "PUBLIC_TRANSPORT"; startTime = "10:00" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8001/internal/courses/recommend `
  -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body)) | ConvertTo-Json -Depth 5
```

자세한 설계와 코드는 루트의 `RAG_다음단계_Phase3-4_가이드_3.md`를 참고한다.

## 테스트

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

테스트는 OpenAI의 실제 API를 호출하지 않는다. 실제 키로 서버를 사용하려면 API 사용 권한과 사용량을 확인하고 Python 서버를 키가 설정된 환경에서 실행해야 한다.

## 지역·관광지 데이터

- 공공데이터포털의 한국관광공사 국문 관광정보 서비스(KorService2)의 법정동 지역 코드와 지역기반 관광지 목록을 사용한다. 지역 목록은 Python 프로세스에서 한 시간 동안 캐시한다.
- 대화 화면에서 지역을 선택하고 관광지를 가져오면, **현재 조회한 페이지**의 관광지와 지역만 Spring이 기존 `region`, `region_external_code`, `place`에 저장한다. 전국 전체 자동 수집이나 일괄 동기화는 구현하지 않았다.
- 지역 외부 코드는 제공처와 함께 연결하고, 관광지는 `TOURAPI`와 콘텐츠 ID로 구분한다. 기존 다른 출처 장소나 사용자가 관리하는 설명·이미지·비활성화 및 검증 상태는 덮어쓰지 않는다.
- 현재 저장 항목은 관광지 ID, 이름, 주소, 위경도, 전화번호와 지역이며, 상세 설명과 이미지·운영시간·음식점은 수집하지 않는다. 이 Spring 동기화 경로로 들어온 장소는 끝에서 `/internal/places/index`를 통해 Chroma에도 자동으로 들어간다("MariaDB → ChromaDB 적재와 코스 추천" 참고).
- API 인증 실패와 오류는 목록을 빈 결과로 위장하지 않고 화면에 오류로 표시한다. 인증키나 요청 URL은 로그에 남기지 않는다.

## 대화에서 관광지 추천

- 조건을 모두 입력한 뒤 추천 요청을 하거나 조건 확인 질문에 `확인했어`라고 답하면, Spring이 지역에 저장된 활성 관광지 중 최대 30개를 Python에 전달한다. LangChain이 이 후보 ID 중 최대 세 곳을 고른다.
- 장소 이름과 주소는 DB에서 표시하며, AI가 후보에 없는 ID를 반환하면 결과를 거부한다. 지역에 가져온 장소가 없다면 화면의 관광지 가져오기 기능을 안내한다.
- 현재 검색은 DB에 저장된 후보의 이름순 조회이며 벡터 검색이나 RAG가 아니다. 날짜별 일정 및 실시간 운영정보도 제공하지 않는다.
