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
        "질문을 하나만 만들어줘"
        "가벼운 대화 소재 하나를 제공해줌으로써 평소 소통이 잘 되지 않는 가족들의 소통을 증진시킬 수 있도록 하고 싶어."
        "대부분의 가족들이 공감하고 답할 수 있는 너무 길지 않은 분량의 질문이어야 해. "
        "예를 들어, (계절에 맞게)이번 휴가에 가족들과 떠나고 싶은 곳은 어디야? 최근 집밥 중 가장 맛있었던 메뉴는? 부모님이 생각하는 자식(아들,딸 등)은 어떤 사람이야? 와 같은 식으로 질문 제공해줘."
    )

    try:
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
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