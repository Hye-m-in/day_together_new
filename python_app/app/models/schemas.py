# python_app/app/schemas.py
from pydantic import BaseModel

class GoogleTokenRequest(BaseModel):
    id_token: str

class NaverTokenRequest(BaseModel):
    access_token: str

class TokenResponse(BaseModel):
    custom_token: str
