# python_app/app/schemas.py

from pydantic import BaseModel

class TokenRequest(BaseModel):
    # Android → 서버로 보내는 Google ID 토큰을 담는 필드
    id_token: str

class TokenResponse(BaseModel):
    # 서버 → Android 로 반환할 Firebase 커스텀 토큰
    custom_token: str
