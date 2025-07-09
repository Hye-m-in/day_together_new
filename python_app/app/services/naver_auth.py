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
    url = "https://openapi.naver.com/v1/nid/me"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
    }
    async with httpx.AsyncClient() as client:
        resp = await client.get(url, headers=headers)
    data = resp.json()
    if data.get("resultcode") != "00":
        return None
    return data["response"]  # { id, email, name, ... }
