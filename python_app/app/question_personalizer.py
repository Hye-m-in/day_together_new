# File: python_app/app/question_personalizer.py
from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional

from app.services.firebase_client import db
from app.gpt.question_generator import generate_daily_question, client

KST = timezone(timedelta(hours=9))


def get_recent_messages(chat_room_id: str, days: int = 7) -> List[Dict[str, Any]]:
    """
    특정 채팅방의 최근 N일 메시지 가져오기
    messages 컬렉션 구조에 맞게 필드 이름만 맞춰주면 됨.
    """
    now = datetime.now(KST)
    start_time = now - timedelta(days=days)

    query = (
        db.collection("chatRooms")
          .document(chat_room_id)
          .collection("messages")
          .where("createdAt", ">=", start_time)
          .order_by("createdAt")
    )

    docs = query.stream()
    messages: List[Dict[str, Any]] = []

    for doc in docs:
        data = doc.to_dict()
        text = data.get("content") or data.get("text")
        if not text:
            continue
        messages.append(
            {
                "senderId": data.get("senderId"),
                "senderName": data.get("senderName"),
                "text": text,
                "createdAt": data.get("createdAt"),
            }
        )

    return messages


def summarize_week_messages(messages: List[Dict[str, Any]]) -> str:
    """
    지난 1주일 대화를 요약하고,
    주요 주제 / 분위기 / 이벤트 / 구성원 특징을 정리하는 GPT 호출
    """
    if not messages:
        return "지난 1주일 동안 이 채팅방에서는 대화가 거의 없었습니다."

    MAX_MSG = 200
    selected = messages[-MAX_MSG:]

    conversation_text = ""
    for m in selected:
        name = m.get("senderName") or m.get("senderId")
        conversation_text += f"{name}: {m['text']}\n"

    system_prompt = (
        "너는 가족 단톡방의 지난 1주일 대화를 분석해서, "
        "가족의 관심사와 분위기를 요약해주는 분석가야. "
        "반드시 한국어로 대답해."
    )

    user_prompt = f"""
다음은 어떤 가족 채팅방에서 지난 1주일 동안 오간 메시지야.

[대화 내역 시작]
{conversation_text}
[대화 내역 끝]

다음 정보를 한국어로 정리해줘:

1. 지난 1주일 동안 자주 등장한 주제 3~5개 (간단 설명 포함)
2. 특별한 이벤트나 이슈가 있었다면 (시험, 여행, 생일, 취업, 건강 문제 등) 정리
3. 가족 전체 분위기 (예: 시험 준비로 바쁨, 여행 기대, 요즘 다들 피곤해 함 등)
4. 각 가족 구성원의 특징적인 발언 패턴
5. 질문을 만들 때 민감해서 피해야 할 것 같아 보이는 주제가 있다면 적어줘

형식은 자유롭게 서술하되, 항목별로 구분해서 써줘.
"""
    resp = client.chat.completions.create(
        model="gpt-4-turbo",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        max_tokens=600,
        temperature=0.4,
    )
    summary = resp.choices[0].message.content.strip()
    return summary


def get_recent_questions_for_room(chat_room_id: str, limit: int = 10) -> List[str]:
    """
    최근 '오늘의 질문' 텍스트를 가져와서 중복 검사용 리스트로 반환.
    네 프로젝트에서는 daily_questions 컬렉션을 사용.
    daily_questions 문서에 { chatRoomId, question(or body), createdAt } 이 있다고 가정.
    """
    docs = (
        db.collection("daily_questions")
        .where("chatRoomId", "==", chat_room_id)
        .order_by("created_at", direction="DESCENDING")
        .limit(limit)
        .stream()
    )

    questions: List[str] = []
    for doc in docs:
        data = doc.to_dict()
        # 필드 이름이 body면 여기서 바꿔줘도 됨
        q = data.get("question") or data.get("body")
        if q:
            questions.append(q)
    return questions


def generate_personalized_daily_question_for_room(
    chat_room_id: str,
    chat_room_name: str,
    debug: bool = False,
) -> Dict[str, Any]:
    """
    1) 최근 1주 메시지 조회
    2) GPT로 요약 생성
    3) 최근 질문 목록 조회
    4) generate_daily_question 에 weekly_summary 넘겨서 개인화 질문 생성
    """
    messages = get_recent_messages(chat_room_id, days=7)
    weekly_summary = summarize_week_messages(messages)

    recent_questions = get_recent_questions_for_room(chat_room_id, limit=10)

    question_data = generate_daily_question(
        chat_room_name=chat_room_name,
        recent_questions=recent_questions,
        weekly_summary=weekly_summary,   # ← question_generator 쪽에 이 인자 추가되어 있어야 함!
        debug=debug,
    )

    return question_data

""""""
def publish_today_question_for_room(chat_room_id: str, chat_room_name: str):
    """
    daily_questions 컬렉션에 '내일(혹은 targetDate)의 질문'을 저장.
    실제 채팅방 messages에 넣는 로직은 너가 이미 갖고 있는
    '발행 job'에서 daily_questions를 읽어서 chatRooms/{roomId}/messages 로 복사.
    """
    question_data = generate_personalized_daily_question_for_room(
        chat_room_id=chat_room_id,
        chat_room_name=chat_room_name,
        debug=False,
    )

    now = datetime.now(KST)
    # 내일 아침 9시에 발행할 거라면 targetDate를 내일 날짜로 두는 식으로 쓸 수 있음
    target_date = (now + timedelta(days=1)).date().isoformat()

    db.collection("daily_questions").add(
        {
            "chatRoomId": chat_room_id,
            "title": "오늘의 질문",
            "question": question_data.get("question"),
            "category": question_data.get("category"),
            "tone": question_data.get("tone"),
            "timeframe": question_data.get("timeframe"),
            "createdAt": now,
            "targetDate": target_date,          # 발행 예정일 (이미 쓰고 있으면 그대로)
            "source": "personalized_weekly",
        }
    )
