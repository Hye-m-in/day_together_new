# File: python_app/app/main.py
import sys
import os
import httpx
import pytz
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, WebSocket,Body
from fastapi.middleware.cors import CORSMiddleware
from .services.firebase_client import db, fb_auth
from apscheduler.triggers.interval import IntervalTrigger

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
from .models.schemas import GoogleTokenRequest, NaverTokenRequest, TokenResponse

from fastapi.middleware.cors import CORSMiddleware


# ---------------------------------------------------------
# 1) 스케줄러 & 질문 생성 함수
# ---------------------------------------------------------
import traceback
seoul_tz = pytz.timezone("Asia/Seoul")
scheduler = BackgroundScheduler(timezone=seoul_tz)


def generate_and_store_daily_question():
    print("[Question] 질문 생성 job 실행됨")
    tomorrow = (datetime.now(seoul_tz) + timedelta(days=1)).date()
    # 테스트용 (오늘 날짜로 저장)
    #tomorrow = datetime.now(seoul_tz).date()
    try:
        chat_rooms = db.collection("chatRooms").stream()
    except Exception as e:
        print(f"[Scheduler] chatRooms 조회 실패: {e}")
        traceback.print_exc()
        return
    
    for room in chat_rooms:
            room_id = room.id
            family_name = room.to_dict().get("familyName", "우리 가족")
            
            print(f"[Scheduler] 처리 시작: room_id={room_id}, family_name={family_name}")

            try:
                # 1) GPT 질문 생성
                data = qg.generate_daily_question(
                    chat_room_name=family_name,
                    recent_questions=[],
                )
                print(f"[Scheduler] GPT 질문 생성 성공: room_id={room_id}")

                # 2) daily_questions 저장
                db.collection("daily_questions").add({
                    "chatRoomId": room_id,
                    "question": data["question"],
                    "category": data["category"],
                    "tone": data["tone"],
                    "timeframe": data["timeframe"],
                    "created_at": firestore.SERVER_TIMESTAMP,
                    "target_date": tomorrow.isoformat(),
                    "sent_to_chat": False, # 채팅방에 출력 여부
                })
                print(f"[Scheduler] daily_questions 저장 완료: room_id={room_id}")

                """
                6## 3) messages 저장
                db.collection("chatRooms").document(room_id).collection("messages").add({
                    "sender": "system",
                    "content": data["question"],
                    "timestamp": firestore.SERVER_TIMESTAMP,
                    "type": "system",
                    "target_date": tomorrow.isoformat(),
                })
                print(f"[Scheduler] messages 저장 완료: room_id={room_id}")
                """


            except Exception as e:
                print(f"[Scheduler] room_id={room_id} 처리 중 에러: {e}")
                traceback.print_exc()
                # continue 해서 다른 방은 계속 돌게
                continue

    print("[Scheduler] All daily questions generated (job 완료)")

# ---------------------------------------------------------
# 2) FastAPI 앱 생성 (단 한 번만!)
# ---------------------------------------------------------
app = FastAPI()
# ---------------------------------------------------------
# 3) 앱 시작/종료 이벤트에 스케줄러 연결
# ---------------------------------------------------------
# 로컬/서버 공통으로 안전하게 종료하도록 등록
atexit.register(lambda: scheduler.shutdown(wait=False))

@app.on_event("startup")
async def on_startup():
    print("[App] startup event 진입")

    # 1) 테스트용: 1분마다 질문 생성 (target_date는 위에서 오늘로 바꿔두면 바로 테스트 가능)
    if not scheduler.running:
        scheduler.add_job(
            generate_and_store_daily_question,
            CronTrigger(hour=22, minute=0, timezone=seoul_tz)
            #IntervalTrigger(minutes=1, timezone=seoul_tz)  # 1분마다 실행

        )

        # 2) 테스트용: 1분마다 오늘 질문 발행
        scheduler.add_job(
            publish_all_today_questions_job,
            CronTrigger(hour=9, minute=0, timezone=seoul_tz)
            #IntervalTrigger(minutes=1, timezone=seoul_tz)
        )

        scheduler.start()
        print("[Scheduler] Started")

@app.on_event("shutdown")
async def on_shutdown():
    if scheduler.running:
        scheduler.shutdown()
        print("[Scheduler] Stopped")


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


@app.get("/health")
def health():
    return{"ok": True}

@app.get("/")
def home():
    return {"message": "DayTogether API is running"}

@app.websocket("/ws")
async def ws_echo(ws: WebSocket):
    await ws.accept()
    await ws.send_text("connected")
    try:
        while True:
            msg = await ws.receive_text()
            await ws.send_text(f"echo: {msg}")
    except Exception:
        await ws.close()


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
    2) Firebase 사용자 확인 (있으면 로그인, 없으면 가입)
    3) Firestore에 프로필 저장
    4) Firebase 커스텀 토큰 생성 및 반환
    """
    try:
        # 1. Google 토큰 검증
        request = google.auth.transport.requests.Request()
        id_info = google.oauth2.id_token.verify_oauth2_token(
            body.id_token, request
        )

        uid     = id_info.get("sub")
        email   = id_info.get("email")
        name    = id_info.get("name")
        picture = id_info.get("picture")

        # 2. Firebase 사용자 확인
        try:
            user_record = fb_auth.get_user_by_email(email)
            print(f"[DEBUG] Found existing user for {email}, uid={user_record.uid}")
        except fb_auth.UserNotFoundError:
            # 신규 회원가입 처리
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

        # 3. Firestore에 프로필 저장 (신규든 기존이든 항상 업데이트)
        user_ref = db.collection("users").document(uid)
        user_ref.set({
            "email":    email,
            "name":     name,
            "profile_image": picture,
            "invitedChatRoomId": None, # Firestore에서 null로 저장됨
            "position" : None
        }, merge=True)
        

        # 4. 커스텀토큰 생성 및 반환
        custom_token_bytes = fb_auth.create_custom_token(uid)
        custom_token = custom_token_bytes.decode("utf-8")
        return TokenResponse(custom_token=custom_token)

    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid Google ID token: {e}")
    except HTTPException:
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
        #이미 존재하면 -> 로그인 처리
        print(f"[DEBUG] Found existing user for {email}, uid={user_record.uid}")
        
    except fb_auth.UserNotFoundError:
        #  신규 회원가입 처리
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
            "email":         email.get("email"),
            "name":          profile.get("name"),
            "nickname":      profile.get("nickname"),
            "profile_image": profile.get("profile_image"),
            "invitedChatRoomId" : None,
            "position" : None
        }, merge=True)
        print(f"[DEBUG] Firestore 저장 완료: users/{uid}")

    # 3. 커스텀 토큰 생성 및 반환
    print(f"[DEBUG] Generating custom token for {uid}")
    custom_token_bytes = fb_auth.create_custom_token(uid)
    custom_token = custom_token_bytes.decode("utf-8")
    print(f"[DEBUG] Returning custom token for {uid}")
    return TokenResponse(custom_token=custom_token)

# ----------gpt: 오늘 질문을 채팅방에 발행하는 API ----------
@app.post("/chat-rooms/{room_id}/publish-today-question")
def publish_today_question(room_id: str):
    '''
    1) daily_questions 에서 오늘(room_id)의 질문 1개 찾고
    2) 아직 messages에 안 보냈고, 9시 이후라면 system 메시지로 추가
    3) sent_to_chat 플래그 업데이트
    '''

    now = datetime.now(seoul_tz)
    today = now.date()
    today_str = today.isoformat()

    # 9시 이전이면 차단
    if now.hour < 9:
        raise HTTPException(403, "아직 질문 공개 시간이 아닙니다.")

    # 오늘 이 방의 질문 조회
    qs = (
        db.collection("daily_questions")
          .where("chatRoomId", "==", room_id)
          .where("target_date", "==", today_str)
          .limit(1)
          .stream()
    )

    doc = next(qs, None)
    if not doc:
        raise HTTPException(404, "오늘 질문이 없습니다.")

    data = doc.to_dict()
    question = data.get("question")

    if not question:
        raise HTTPException(500, "질문 데이터가 비어 있습니다.")
        

    # 한 번도 채팅방에 안 보냈을 경우만 메시지 생성
    if not data.get("sent_to_chat"):
        # messages에 system 메시지 추가
        db.collection("chatRooms").document(room_id).collection("messages").add({
            "sender": "system",
            "content": question,
            "timestamp": firestore.SERVER_TIMESTAMP,
            "type": "system",
            "target_date": today_str,
        })

        # daily_questions 플래그 업데이트
        doc.reference.update({
            "sent_to_chat": True,
            "sent_at": firestore.SERVER_TIMESTAMP,
        })

    # 안드로이드용 응답
    return {
        "roomId": room_id,
        "question": question,
        "target_date": today_str,
    }


def publish_today_question_for_room(room_id: str):
    now = datetime.now(seoul_tz)
    today_str = now.date().isoformat()

    qs = (
        db.collection("daily_questions")
          .where("chatRoomId", "==", room_id)
          .where("target_date", "==", today_str)
          .limit(1)
          .stream()
    )

    doc = next(qs, None)
    if not doc:
        print(f"[PublishJob] room_id={room_id} 오늘 질문 없음")
        return

    data = doc.to_dict()
    question = data.get("question")
    if not question:
        print(f"[PublishJob] room_id={room_id} 질문 없음")
        return

    if data.get("sent_to_chat") is True:
        print(f"[PublishJob] room_id={room_id} 이미 발행됨, 스킵")
        return

    # messages에 system 메시지 추가
    db.collection("chatRooms").document(room_id).collection("messages").add({
        "sender": "system",
        "content": question,
        "timestamp": firestore.SERVER_TIMESTAMP,
        "type": "system",
        "target_date": today_str,
    })

    # daily_questions 플래그 업데이트
    doc.reference.update({
        "sent_to_chat": True,
        "sent_at": firestore.SERVER_TIMESTAMP,
    })

    print(f"[PublishJob] room_id={room_id} 오늘 질문 발행 완료")

def publish_all_today_questions_job():
    print("[PublishJob] 전체 방 오늘 질문 발행 job 시작")

    try:
        chat_rooms = db.collection("chatRooms").stream()
    except Exception as e:
        print(f"[PublishJob] chatRooms 조회 실패: {e}")
        traceback.print_exc()
        return

    for room in chat_rooms:
        room_id = room.id
        publish_today_question_for_room(room_id)

    print("[PublishJob] 전체 방 오늘 질문 발행 job 완료")


#----------gpt------------
"""@app.post("/daily-question")
async def daily_question():
    seoul_tz = pytz.timezone("Asia/Seoul")
    tomorrow = (datetime.now(seoul_tz) + timedelta(days=1)).date()

    # 모든 chatRooms 가져오기
    chat_rooms = db.collection("chatRooms").stream()

    results = {}

    for room in chat_rooms:
        room_id = room.id
        #roomid 가져오고 각 가족채팅방 이름도 가져오는데 이름이 없다면 이름없는방으로 ->무조건 있어야 함 
        chat_room_name= room.to_dict().get("chatRoomName", "이름없는방")

    # 최근 질문 5개 (이 방 전용) 가져오기
        recent_docs = (
            db.collection("chatRooms")
              .document(room_id)
              .collection("messages")
              .where("type", "==", "system")
              .order_by("timestamp", direction=firestore.Query.DESCENDING)
              .limit(5)
              .stream()
        )
        recent_questions = [doc.to_dict().get("content", "") for doc in recent_docs]


        # GPT 질문 생성
        data = qg.generate_daily_question(
            chat_room_name=chat_room_name, 
            recent_questions=recent_questions
        )

        # 1) daily_questions 컬렉션 저장 (로그/통계용)
        db.collection("daily_questions").add({
            "room_id": room_id,
            "question": data["question"],
            "category": data["category"],
            "tone": data["tone"],
            "timeframe": data["timeframe"],
            "created_at": firestore.SERVER_TIMESTAMP,
            "target_date": tomorrow.isoformat()
        })
        # 2) 해당 방 messages에 저장
        db.collection("chatRooms").document(room_id).collection("messages").add({
            "sender": "system",
            "content": data["question"],
            "timestamp": firestore.SERVER_TIMESTAMP,
            "type": "system",
            "target_date": tomorrow.isoformat()
        })       

        print(f"[DailyQuestion API] 질문 저장 완료 → roomId={room_id}")
        results[room_id] = data


    return results"""