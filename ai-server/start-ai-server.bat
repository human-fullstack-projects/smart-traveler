@echo off
setlocal
cd /d "%~dp0"

if not exist ".env" (
    echo Missing .env file in ai-server.
    exit /b 1
)

set "OPENAI_API_KEY="
".venv\Scripts\python.exe" -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --env-file .env --log-level warning --no-access-log
