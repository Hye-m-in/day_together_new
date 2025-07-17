# python_app/app/services/naver_auth.py

import os
import httpx
from dotenv import load_dotenv

# ① .env 로드 (이미 init에서 한 번 했다면 생략 가능)
load_dotenv()

# ② 환경변수에서 클라이언트 ID/SECRET 읽기
NAVER_CLIENT_ID     = os.getenv("NAVER_CLIENT_ID")
NAVER_CLIENT_SECRET = os.getenv("NAVER_CLIENT_SECRET")
if not NAVER_CLIENT_ID or not NAVER_CLIENT_SECRET:
    raise RuntimeError("❌ NAVER_CLIENT_ID/SECRET 환경변수가 설정되지 않았습니다!")

async def verify_naver_token(access_token: str) -> dict | None:
    """
    네이버 OAuth 액세스 토큰을 검증하고, 프로필(response) 데이터를 반환합니다.
    실패 시 None을 반환.
    """

    #1 네이버 API 프로필 조회 URL설정
    url = "https://openapi.naver.com/v1/nid/me"

    #2. 요청 헤더 구성 (OAuth2 표준 방식 중 하나로 Bearer 토큰을 머리말에 붙여서 서버에 보냄)
    #-> 서버가 이 토큰을 보고 사용자가 로그인 상태인지 확인 후 프로필을 돌려주거나 거절
    headers = {
        # Bearer 토큰 방식으로 액세스 토큰 전달
        "Authorization": f"Bearer {access_token}",

        # 애플리케이션 식별용
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
    }

    #3. 비동기 HTTP 클라이언트를 사용해 GET 요청
    async with httpx.AsyncClient() as client:
        resp = await client.get(url, headers=headers)

    #4. 응답 본문을 JSON으로 파싱
    data = resp.json()

    #5. resultcode 값이 '00'이 아니면 검증 실패로 간주하고 None 반환
    if data.get("resultcode") != "00":
        return None
    
    #6. 검증 성공 시 사용자 프로필 정보를 반환
    return data["response"]  # { id, email, name, ... }
