# python_app/app/firebase_admin_init.py

from dotenv import load_dotenv
import os
import pathlib
import firebase_admin
from firebase_admin import credentials, initialize_app

# ① .env 파일 읽어오기 (app/ 디렉터리 기준)
dotenv_path = pathlib.Path(__file__).parent / ".env"
load_dotenv(dotenv_path=dotenv_path)   # ← 이 줄 반드시 추가

# ① 환경변수 또는 기본값으로 JSON 키 경로 지정
# SERVICE_ACCOUNT_PATH = os.getenv(
#     "GOOGLE_APPLICATION_CREDENTIALS",
#     "serviceAccountKey.json"  # 실제 위치에 맞게 수정
# )

SERVICE_ACCOUNT_PATH = os.getenv(
    "FIREBASE_SERVICE_ACCOUNT_PATH",
    str(pathlib.Path(__file__).parent / "serviceAccountKey.json")
)

print(f"▶ Firebase Admin 초기화 키 경로: {SERVICE_ACCOUNT_PATH}") #경로 잘 되어있나 찍어보아요요


# ② 자격 증명 객체 생성
cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)

# ③ Admin SDK 초기화 (Firestore, Auth 등 Admin API 사용 가능)
default_app = initialize_app(cred)

print("▶ Firebase Admin SDK 초기화 완료")