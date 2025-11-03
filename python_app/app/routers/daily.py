# app/routers/daily.py
from fastapi import APIRouter
from google.cloud import firestore
from datetime import datetime, timedelta
import pytz

from app.gpt.question_generator import generate_daily_question as gen_q
router = APIRouter()
db = firestore.Client()


# * post /daily-question 을 호출하면 가족방(=채팅방)마다 내일 질문을 하나씩 생성해서 DB에 저장
# async를 쓰는 이유는 비동기로 I/O가 많은 작업에 적합하기 때문임 (우리 플젝의 경우 firestore 호출이 다수)
@router.post("/daily-question")
async def daily_question():
    """
    목적
        - 모든 chatRoom을 순회하며 '내일자' 질문을 한 번씩 생성하여 저장
        - daily_questions(로그/통계) + 각 방 messages를 동시에 기록
    반환
        - {room_id : {ok/skipped/error...}} 형태로 방별 처리 결과 요약 예정
    """

    # 내일자 타깃 날짜 계산 - 서버가 해외 리전에 있어도 한국 시간을 기준으로 내일을 계산
    seoul_tz = pytz.timezone("Asia/Seoul")
    target_date = (datetime.now(seoul_tz) + timedelta(days=1)).date().isoformat()

    # 모든 가족방(채팅방) 목록 가져오기
    rooms = db.collection("chatRooms").stream()


    # 방 하나씩 순회 처리
    results = {}
    for room in rooms:
        room_id = room.id
        doc = room.to_dict() or {}
        name =  doc.get("chatRoomName", "이름없는방")
        field_room_id = doc.get("chatRoomId")

        # 0) ID-필드 불일치 검출(있으면 로그만 남기고, 문서 ID를 정으로 사용)
        if field_room_id and field_room_id != room_id:
            print(f"[WARN] chatRoomId mismatch: docId = {room_id} field={field_room_id}")

        # 1) 중복 방지 : 이미 해당 날짜 생성됐는지 확인
        existed = (
            db.collection("daily_questions")
            .where("room_id", "==", room_id)
            .where("target_date", "==", target_date)
            .limit()
            .stream()
        )

        if any(True for _ in existed):
            results[room_id] = {"skipped": True, "reason": "already_exists"}
            continue

        # 2) 최근 질문 5개
        messages_ref = (db.collection("chatRooms").document(room_id)
                        .collection("messages"))
        recent_docs = (messages_ref
                       .where("type", "==", "system")
                       .order_by("timestamp", direction=firestore.Query.DESCENDING)
                       .limit(5)
                       .stream())
        recent_questions = [d.to_dict().get("content", "") for d in recent_docs]

        # 3) GPT 생성
        data = gen_q(chat_room_name=name, recent_questions=recent_questions)

        # 4) 배치 커밋
        batch = db.batch()
        dq_ref = db.collection("daily_questions").document()
        batch.set(dq_ref, {
            "room_id": room_id,
            "chat_room_name": name,
            "question": data["question"],
            "category": data.get("category"),
            "tone": data.get("tone"),
            "timeframe": data.get("timeframe"),
            "target_date": target_date,
            "created_at": firestore.SERVER_TIMESTAMP,
        })
        msg_ref = messages_ref.document()
        batch.set(msg_ref, {
            "sender": "system",
            "content": data["question"],
            "type": "system",
            "target_date": target_date,
            "timestamp": firestore.SERVER_TIMESTAMP,
        })
        batch.commit()

        results[room_id] = {"ok": True, **data}

    return results


