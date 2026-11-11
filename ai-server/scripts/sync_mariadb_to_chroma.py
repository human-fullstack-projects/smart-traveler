"""MariaDB → Chroma 적재 (명령줄용). ai-server 폴더에서 실행:
    .\\.venv\\Scripts\\python.exe -m scripts.sync_mariadb_to_chroma 26350
서버의 POST /internal/places/sync 와 같은 일을 한다.
"""
import pathlib
import sys
from dotenv import load_dotenv

load_dotenv(pathlib.Path(__file__).resolve().parent.parent / ".env")

from app.rag import places, sync_from_mariadb

codes = sys.argv[1:] or ["26350"]
for code in codes:
    print(code, sync_from_mariadb(code))
print("Chroma 전체 장소 수:", places.count())
