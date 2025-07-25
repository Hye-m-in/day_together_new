# File: python_app/app/main.py

import os
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from .services.firebase_client import db, fb_auth

import google.auth.transport.requests
import google.oauth2.id_token

from .services.firebase_admin_init import default_app  # 초기화만 호출됨
from .models.schemas import GoogleTokenRequest, NaverTokenRequest, TokenResponse  # <<< 수정: TokenRequest → NaverTokenRequest로 분리

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

# ----- 구글 로그인 핸들러 -----
@app.post("/google-login", response_model=TokenResponse)
def google_login(body: GoogleTokenRequest = Body(..., description="Google ID 토큰을 담은 객체")):
    """
    1) Android 클라이언트가 보낸 Google ID 토큰 검증
    2) 이메일 중복 체크 : 같은 이메일로 이미 가입된 계정이 있다면 오류 반환
    3) Firebase Authentication에 사용자 레코드 생성(없다면)
    4) Firestore에 프로필 저장
    5) Firebase 커스텀 토큰 생성 및 반환
    """
    try:
        #1. Google 토큰 검증
        request = google.auth.transport.requests.Request()
        id_info = google.oauth2.id_token.verify_oauth2_token(
            body.id_token, request
        )

        uid     = id_info.get("sub")
        email   = id_info.get("email")
        name    = id_info.get("name")
        picture = id_info.get("picture")


        #2. 이메일 중복 체크
        try:
            fb_auth.get_user_by_email(email)
            #이미 같은 이메일로 가입된 계정이 있으면
            raise HTTPException(status_code=400, detail="이미 등록된 이메일 입니다.")
        except fb_auth.UserNotFoundError:
            #가입된 계정이 없으면 계속 진행
            pass


        #3. Authentication에 사용자 생성(없다면)
        try:
            fb_auth.get_user(uid)
            print(f"[DEBUG] Auth user {uid} already exists")
        except (fb_auth.UserNotFoundError, ValueError) as e:
            print(f"[DEBUG] Creating Auth user {uid}: {e}")
            fb_auth.create_user(
                uid=uid,
                email=email,
                display_name=name,
                photo_url=picture
            )

        #4. Firestore에 프로필 저장
        user_ref = db.collection("users").document(uid)
        user_ref.set({
            "email":    email,
            "name":     name,
            "profile_image": picture,
        }, merge=True)
        
        #5. 커스텀토큰 생성 및 반환
        custom_token_bytes = fb_auth.create_custom_token(uid)
        custom_token = custom_token_bytes.decode("utf-8")
        return TokenResponse(custom_token=custom_token)

    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid Google ID token: {e}")
    except HTTPException:
        #위에서 던진 400에러는 그대로 통과
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Server error: {e}")




# ----- 네이버 로그인 핸들러 -----
@app.post("/naver-login", response_model=TokenResponse)
async def naver_login(body: NaverTokenRequest): 
    print("[DEBUG] /naver-login 호출됨, body:", body)
    """
    1) Android 클라이언트가 보낸 Naver access token 검증
    2) 이메일 중복 체크 : 같은 이메일로 이미 가입된 계정이 있다면 오류 반환
    3) Firebase Authentication에 사용자 레코드 생성
    4) Firebase Admin SDK로 해당 uid에 대한 커스텀 토큰 생성
    5) 커스텀 토큰을 JSON으로 반환
    """

    #1. 액세스 토큰 유효성 체크
    if not body.access_token:
        raise HTTPException(status_code=400, detail="access_token이 필요합니다.")

    profile = await verify_naver_token(body.access_token)
    if not profile:
        raise HTTPException(status_code=401, detail="유효하지 않은 Naver 토큰입니다.")
    
    uid = f"naver_{profile['id']}"
    email = profile.get("email")

    #2. 이메일 중복 체크
    try:
        fb_auth.get_user_by_email(email)
        #이미 같은 이메일로 가입한 계정이 있으면
        raise HTTPException(status_code=400, detail="이미 등록된 이메일입니다.")
    except fb_auth.UserNotFoundError:
        #가입된 계정이 없으면 계속 진행
        pass
    
    #3. Authentication에 사용자 생성 (없으면)
    try:
       fb_auth.get_user(uid)
       print(f"[DEBUG] Auth user {uid} already exists")
    except (fb_auth.UserNotFoundError, ValueError) as e:
       print(f"[DEBUG] Creating Auth user {uid}: {e}")
       fb_auth.create_user(
           uid=uid,
           email=profile.get("email"),
           display_name=profile.get("name")
       )

    #4. Firestore에 프로필 저장
    print(f"[DEBUG] Writing Firestore users/{uid}")
    user_ref = db.collection("users").document(uid)
    user_ref.set({
        "email":         profile.get("email"),
        "name":          profile.get("name"),
        "nickname":      profile.get("nickname"),
        "profile_image": profile.get("profile_image"),
        # 필요 시 추가 필드…
    }, merge=True)

    #*잘 저장되었는지 로그 남기기*
    print(f"[DEBUG] Firestore 저장 완료: users/{uid}")


    #5. 커스텀 토큰 생성 및 반환
    print(f"[DEBUG] Generating custom token for {uid}")
    custom_token_bytes = fb_auth.create_custom_token(uid)
    custom_token = custom_token_bytes.decode("utf-8")
    print(f"[DEBUG] Returning custom token for {uid}")
    return TokenResponse(custom_token=custom_token)
