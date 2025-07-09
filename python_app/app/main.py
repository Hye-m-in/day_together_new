# File: python_app/app/main.py

import os
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import auth as fb_auth
import google.auth.transport.requests
import google.oauth2.id_token

from .firebase_admin_init import default_app  # 초기화만 호출됨
from .schemas import GoogleTokenRequest, NaverTokenRequest, TokenResponse  # <<< 수정: TokenRequest → NaverTokenRequest로 분리

# Naver 검증 로직 인라인
load_dotenv()
NAVER_CLIENT_ID     = os.getenv("NAVER_CLIENT_ID")
NAVER_CLIENT_SECRET = os.getenv("NAVER_CLIENT_SECRET")
if not NAVER_CLIENT_ID or not NAVER_CLIENT_SECRET:
    raise RuntimeError("NAVER_CLIENT_ID/SECRET 설정이 필요합니다!")

async def verify_naver_token(access_token: str) -> dict | None:
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
    return data["response"]


app = FastAPI()

# CORS 설정: Android 에뮬레이터/디바이스에서 호출 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["POST"],
    allow_headers=["*"],
)

@app.post("/google-login", response_model=TokenResponse)
def google_login(body: GoogleTokenRequest):
    """
    1) Android 클라이언트가 보낸 Google ID 토큰 검증
    2) Firebase Admin SDK로 해당 uid에 대한 커스텀 토큰 생성
    3) 커스텀 토큰을 JSON으로 반환
    """
    try:
        request = google.auth.transport.requests.Request()
        id_info = google.oauth2.id_token.verify_oauth2_token(
            body.id_token, request
        )
        uid = id_info.get("sub")
        custom_token_bytes = fb_auth.create_custom_token(uid)
        custom_token = custom_token_bytes.decode("utf-8")
        return TokenResponse(custom_token=custom_token)

    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid Google ID token: {e}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Server error: {e}")

@app.post("/naver-login", response_model=TokenResponse)
async def naver_login(body: NaverTokenRequest):  # <<< 수정: TokenRequest → NaverTokenRequest
    """
    1) Android 클라이언트가 보낸 Naver access token 검증
    2) Firebase Admin SDK로 해당 uid에 대한 커스텀 토큰 생성
    3) 커스텀 토큰을 JSON으로 반환
    """
    if not body.access_token:
        raise HTTPException(status_code=400, detail="access_token이 필요합니다.")

    profile = await verify_naver_token(body.access_token)
    if not profile:
        raise HTTPException(status_code=401, detail="유효하지 않은 Naver 토큰입니다.")

    uid = f"naver:{profile['id']}"
    custom_token_bytes = fb_auth.create_custom_token(uid)
    custom_token = custom_token_bytes.decode("utf-8")
    return TokenResponse(custom_token=custom_token)
