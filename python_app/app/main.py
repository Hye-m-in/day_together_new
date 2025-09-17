# File: python_app/app/main.py
import sys
import os
import httpx
import pytz
from contextlib import asynccontextmanager
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from .services.firebase_client import db, fb_auth

import google.auth.transport.requests
import google.oauth2.id_token

#GPT 관련 import 문-----------------------------------------
# main.py 위치 기준으로 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from firebase_admin import credentials, firestore, initialize_app
from datetime import datetime, timedelta
from app.gpt import question_generator as qg

#GPT 질문 생성 스케줄러---------------------------------------
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import atexit

from .services.firebase_admin_init import default_app  # 초기화만 호출됨
from .models.schemas import GoogleTokenRequest, NaverTokenRequest, TokenResponse  # <<< 수정: TokenRequest → NaverTokenRequest로 분리

from fastapi.middleware.cors import CORSMiddleware


#스케줄러 초기화
scheduler = BackgroundScheduler()

#--------질문 저장 이벤트 스케줄러--------------
@asynccontextmanager
async def lifespan(app: FastAPI):
    #앱 시작 시 실행
    seoul_tz = pytz.timezone("Asia/Seoul")
    scheduler.add_job(
        generate_and_store_daily_question,
        CronTrigger(hour=22, minute=0, timezone=seoul_tz)
    )
    scheduler.start()
    print("[Scheduler] Started")

    yield #앱 실행 유지

    #앱 종료 시 실행
    scheduler.shutdown()
    print("[Scheduler] Stopped")


def generate_and_store_daily_question():
    try:
        # ✅ 매번 실행 시점 기준으로 내일 날짜 계산 (파일 상단에서 고정하지 않음)
        seoul_tz = pytz.timezone("Asia/Seoul")
        tomorrow = (datetime.now(seoul_tz) + timedelta(days=1)).date()

        # chatRooms 컬렉션 순회
        chat_rooms = db.collection("chatRooms").stream()
        for room in chat_rooms:
            room_id = room.id
            # ✅ room.to.dict() → 오타 수정: room.to_dict()
            family_name = room.to_dict().get("familyName", "우리 가족")

            # GPT로 질문 생성
            data = qg.generate_daily_question(family_name=family_name, recent_questions=[])

            # 1) daily_questions 저장 (로그/통계용)
            db.collection("daily_questions").add({
                "chatRoomId": room_id,
                "question": data["question"],
                "category": data["category"],
                "tone": data["tone"],
                "timeframe": data["timeframe"],
                # ✅ created_at 사용 (date 필드 없음)
                "created_at": firestore.SERVER_TIMESTAMP,
                "target_date": tomorrow.isoformat()
            })

            # 2) chatRooms/{roomId}/messages 저장 (실시간 채팅방 표시용)
            db.collection("chatRooms").document(room_id).collection("messages").add({
                "sender": "system",
                "content": data["question"],
                "timestamp": firestore.SERVER_TIMESTAMP,
                "type": "system",
                "target_date": tomorrow.isoformat()
            })

            print(f"[Scheduler] Daily question saved for room {room_id}")

        print("[Scheduler] All daily questions generated successfully")

    except Exception as e:
        print(f"[Scheduler] Error: {e}")


# 한 번만 생성
app = FastAPI(lifespan=lifespan)

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


# CORS 설정: Android 에뮬레이터/디바이스에서 호출 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 또는 ["http://프론트엔드주소"]
    allow_credentials=True,
    allow_methods=["*"],
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

    #2. Firebase 사용자 확인
    try:
        user_record = fb_auth.get_user_by_email(email)
        #이미 존재 하면 -> 로그인 처리
        print(f"[DEBUG] Found existing user for {email}, uid={user_record.uid}")
        
    except fb_auth.UserNotFoundError:
        # 👉 신규 회원가입 처리
        try:
            fb_auth.get_user(uid)
            print(f"[DEBUG] Auth user {uid} already exists")
        except (fb_auth.UserNotFoundError, ValueError) as e:
            print(f"[DEBUG] Creating Auth user {uid}: {e}")
            fb_auth.create_user(
                uid=uid,
                email=email,
                display_name=profile.get("name")
            )

        # Firestore 프로필 저장 (최초 가입 시)
        print(f"[DEBUG] Writing Firestore users/{uid}")
        user_ref = db.collection("users").document(uid)
        user_ref.set({
            "email":         email,
            "name":          profile.get("name"),
            "nickname":      profile.get("nickname"),
            "profile_image": profile.get("profile_image"),
        }, merge=True)
        print(f"[DEBUG] Firestore 저장 완료: users/{uid}")

    # 3. 커스텀 토큰 생성 및 반환
    print(f"[DEBUG] Generating custom token for {uid}")
    custom_token_bytes = fb_auth.create_custom_token(uid)
    custom_token = custom_token_bytes.decode("utf-8")
    print(f"[DEBUG] Returning custom token for {uid}")
    return TokenResponse(custom_token=custom_token)

#----------gpt------------
@app.post("/daily-question")
async def daily_question():

    seoul_tz = pytz.timezone("Asia/Seoul")
    tomorrow = (datetime.now(seoul_tz) + timedelta(days=1)).date()
    #최근 질문 가져오기
    recent_docs = db.collection("daily_questions").order_by("date", direction="DESCENDING").limit(5).stream()
    recent_questions = [doc.to_dict().get("question", "") for doc in recent_docs]

    # 질문 생성
    data = qg.generate_daily_question(family_name="김씨네가족", recent_questions=recent_questions)

    # Firestore에 저장
    doc_ref = db.collection("daily_questions").document()
    doc_ref.set({
        "question": data["question"],
        "category": data["category"],
        "tone": data["tone"],
        "timeframe": data["timeframe"],
        "created_at" : firestore.SERVER_TIMESTAMP, #생성시각 (로그/참조용)
        "target_date" : tomorrow.isoformat() # 질문이 적용될 날짜
    })

    return data


