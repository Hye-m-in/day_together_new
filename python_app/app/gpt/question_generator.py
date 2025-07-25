import openai
import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

# 환경변수에서 API 키 가져오기
openai.api_key = os.getenv("OPENAI_API_KEY")

def generate_daily_question(family_name: str = '가족') -> str:
    """
    GPT에게 가족에게 물어볼 오늘의 질문을 만들어달라고 요청하는 함수
    """
    prompt = (
        f"{family_name}가족 구성원들끼리 친해질 수 있는 가벼운 질문 하나만 만들어줘."
        "질문은 너무 길지 않게, 누구나 답할 수 있도록 쉽고 따뜻하게 말해줘. "
        "예: '다들 오늘 가장 인상깊거나 기억나는 일은 뭐야?' 같은 느낌으로"
    )

    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "당신은 가족을 가깝게 만드는 따뜻하고 친절한 질문을 만드는 도우미 입니다."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=100,
            temperature=0.0
        )
        question = response["choices"][0]["message"]["content"].strip()
        return question
    
    except Exception as e:
        print("질문 생성 중 오류:", e)
        return "오늘 하루를 나누고 싶은 질문이 아직 준비되지 않았음"
    

if __name__ == "__main__":
    print("지피티야, 오늘의 가족 질문 하나 줘봐")
    question = generate_daily_question("김씨네가족")
    print("질문:", question)