from openai import OpenAI
import os
import random
import json
import re
from difflib import SequenceMatcher
from datetime import datetime
from dotenv import load_dotenv
from typing import List, Optional, Dict, Any, Tuple
import string

# .env 파일 로드 + OpenAI 클라이언트 생성
load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# =============== 유틸 ===============
def normalize(text: str) -> str:
    # 구두점/공백 제거 + 소문자화
    pattern = rf"[{re.escape(string.punctuation)}\s]+"
    t = re.sub(pattern, "", text)
    return t.lower()

def similarity(a: str, b: str) -> float:
    return SequenceMatcher(None, normalize(a), normalize(b)).ratio()

def is_too_similar(new_q: str, old_qs: List[str], threshold: float) -> Tuple[bool, float, str]:
    """가장 높은 유사도와 그 문장을 함께 반환"""
    max_sim, max_q = 0.0, ""
    for q in old_qs:
        s = similarity(new_q, q)
        if s > max_sim:
            max_sim, max_q = s, q
    return (max_sim >= threshold, max_sim, max_q)

# =============== 메인 함수 ===============
def generate_daily_question(
    chat_room_name: str,
    recent_questions: Optional[List[str]] = None,
    #질문 관련 새 인자
    weekly_summary: Optional[str] = None,
    max_retries: int = 3,
    similarity_threshold: float = 0.88,
    temperature: float = 0.9,
    top_p: float = 0.95,
    presence_penalty: float = 0.6,
    frequency_penalty: float = 0.3,
    debug: bool = True,
) -> Dict[str, Any]:
    """
    오늘의 가족 질문 1개 생성(랜덤성+중복차단).
    weekly_summary가 있으면 최근 1주일 대화 내용에 맞게 더 개인화함.
    디버깅 로그를 자세히 출력.
    """
    if recent_questions is None:
        recent_questions = []

    today = datetime.now().strftime("%Y-%m-%d (%a)")
    recent_hint = " / ".join(recent_questions[-3:]) if recent_questions else None

    last_error = None
    for attempt in range(1, max_retries + 1):
        if debug:
            print(f"\n[시도 {attempt}/{max_retries}] ---------------------------")
        system, user = build_prompt(chat_room_name, today, recent_hint, weekly_summary)

        try:
            resp = client.chat.completions.create(
                model="gpt-4-turbo",
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
                temperature=temperature,
                top_p=top_p,
                presence_penalty=presence_penalty,
                frequency_penalty=frequency_penalty,
                max_tokens=200,
            )
            content = resp.choices[0].message.content.strip()

            if debug:
                print("[원문 응답(JSON 파싱 전)]")
                print(content)

            # JSON 파싱
            try:
                data = json.loads(content)
            except json.JSONDecodeError as je:
                last_error = f"JSONDecodeError: {je}"
                if debug:
                    print("[이유] JSON 파싱 실패 → 재시도")
                continue

            q = (data.get("question") or "").strip()
            if not q:
                last_error = "빈 question 필드"
                if debug:
                    print("[이유] question이 비어있음 → 재시도")
                continue

            # 과도한 길이/문장 수 보정
            sentences = re.split(r"(?<=[.!?！？｡。])\s+", q)
            if len(sentences) > 2:
                q = " ".join(sentences[:2]).strip()
                data["question"] = q
                if debug:
                    print("[보정] 문장 수 > 2 → 앞 2문장으로 축약")

            # 유사도 검사
            too_sim, max_sim, max_q = is_too_similar(q, recent_questions, similarity_threshold)
            if debug:
                print(f"[유사도 검사] max_sim={max_sim:.4f} (임계 {similarity_threshold})")
                if max_q:
                    print(f" - 가장 유사한 기존 질문: {max_q}")

            if too_sim:
                last_error = f"유사도 초과({max_sim:.3f} ≥ {similarity_threshold})"
                if debug:
                    print("[이유] 최근 질문과 지나치게 유사 → 재시도")
                continue

            if debug:
                print("[성공] 최종 질문 생성 완료")
            return data

        except Exception as e:
            last_error = repr(e)
            if debug:
                print(f"[예외] API 예외 발생 → 재시도: {last_error}")
            continue

    # 모든 시도 실패 → fallback
    if debug:
        print("\n[결과] 모든 재시도 실패. Fallback 반환")
        if last_error:
            print(f"[마지막 에러] {last_error}")
    return {
        "category": "fallback",
        "tone": "따뜻하게",
        "timeframe": "오늘",
        "question": "오늘 하루 중 가장 기분 좋았던 순간을 가족과 나눠볼까요?",
        "follow_up": "다른 가족에게도 오늘 기분 좋았던 순간이 있었는지 물어봐 주세요."
    }


# =============== 프롬프트 빌더 ===============
def build_prompt(
    chat_room_name: str,
    today: str,
    recent_topics_hint: Optional[str],
    #질문 개인화를 위한 인자 주입
    weekly_summary: Optional[str] = None
) -> Tuple[str, str]:
    categories = ["가족대화","일상","취미·문화","주말계획","미래·새도전"]
    tones = ["따뜻하게","유쾌하게","잔잔하게","호기심을 담아","격려하는 톤으로"]
    timeframes = ["오늘","이번주","이번달","최근","어릴 때","곧 다가올 주말"]
    formats = [
        "개방형 한 문장 질문",
        "선호 비교를 유도하는 질문",
        "구체적 예시를 유도하는 질문"
    ]
    cat = random.choice(categories)
    tone = random.choice(tones)
    tf = random.choice(timeframes)
    form = random.choice(formats)

    recent_clause = ""
    if recent_topics_hint:
        recent_clause = (
            f"최근에 다뤘던 주제는 피하고({recent_topics_hint}), 새로운 각도로 질문하세요."
        )


    #주간 요약 컨텍스트
    weekly_block = ""
    if weekly_summary:
        weekly_block = f"""
[최근 1주일 대화 요약]
{weekly_summary}

- 위 요약에서 나온 관심사, 이벤트, 분위기를 자연스럽게 반영하되,
  특정 인물 하나를 콕 집어 언급하기보다는, 방 전체가 공감할 수 있는 질문으로 만들어 주세요.
- 민감하거나 피해야 할 주제가 있다면 그 부분은 피해서 질문을 만들어 주세요.
"""

    system = (
        "역할: 가족을 가깝게 만드는 따뜻하고 친절한 질문 생성기.\n"
        "출력은 오직 JSON 한 덩어리만. JSON 외 텍스트/설명/코드블록 금지.\n"
        "정치/의학 조언/갈등 유발/예·아니오 닫힌 질문 금지.\n"
        "질문은 정확히 1개만 생성."
    )
    user = f"""
[목표]
- {chat_room_name}의 소통을 부드럽게 시작할 개방형 질문 1개 생성

[문체/어미]
- 자연스러운 구어체, ~해요/~했어요/~보셨어요? 와 같은 형태
- '있나요?' 같은 조사+의문문 패턴은 피하기
- 1~2문장, 문장당 25자 내외

[금지 어구(반드시 피함)]
- "가족들과", "가족끼리", "함께 한", "같이 한", "우리 가족", "다 함께"

[권장 표현]
- "요즘 당신/각자/너의 하루에서…", "느낀 점"
- 구체 단서 1개 포함(시간대·장소·상황·사례)

[출력 형식(JSON만)]
{{
  "category": "{cat}",
  "tone": "{tone}",
  "timeframe": "{tf}",
  "question": "<개인의 현재 경험을 묻는 개방형 질문 1개>",
  "follow_up": "<상대에게 자연스럽게 되묻는 1문장 제안>"
}}

[참고]
- 오늘 날짜: {today}
- 한국어로 작성

{weekly_block}
{recent_clause}
""".strip()
    return system, user


# =============== 실행 예시 ===============
if __name__ == "__main__":
    recent = [
        "이번 주말에 가족끼리 같이 해보고 싶은 작은 계획이 있나요?",
        "요즘 즐겨 먹는 간식이나 음식이 있다면 가족과 함께 나눠보고 싶은 건 무엇인가요?"
    ]
    data = generate_daily_question(
        chat_room_name="김씨네가족",
        recent_questions=recent,
        max_retries=3,
        similarity_threshold=0.88,  # 필요 시 0.90~0.92로 완화/강화
        debug=True,
    )
    print("\n=== 최종 출력 ===")
    print("카테고리:", data.get("category"))
    print("톤:", data.get("tone"))
    print("시간축:", data.get("timeframe"))
    print("질문:", data.get("question"))
