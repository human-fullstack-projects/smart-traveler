"""로컬 user1 마이페이지 시험 일정의 장소를 지역별 place 자료로 교체한다.

기본은 미리보기. --apply로 적용하며 이전 일정과 복구 SQL을 logs에 저장한다.
예약의 날짜/금액/숙소, 사용자 작성 후기 스냅샷은 변경하지 않는다.
"""
from pathlib import Path
import argparse
import datetime
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]

def literal(value):
    if value is None:
        return 'NULL'
    # DB 연결 문자셋에 의존하지 않는 UTF-8 문자열 리터럴.
    return "CONVERT(X'" + str(value).encode('utf-8').hex() + "' USING utf8mb4)"

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    options = parser.parse_args()
    properties = (ROOT/'smarttraveler/src/main/resources/application.properties').read_text(encoding='utf-8-sig')
    def setting(name):
        match = re.search(r'^spring.datasource.'+name+r'=\$\{([^:}]+):([^}]*)\}', properties, re.M)
        if not match:
            raise RuntimeError('DB 설정을 확인하세요: '+name)
        return os.environ.get(match[1], match[2])
    url = re.match(r'jdbc:mariadb://([^:/?]+)(?::(\d+))?/([^?]+)', setting('url'))
    if not url or url[1] not in ('localhost', '127.0.0.1'):
        raise RuntimeError('이 스크립트는 로컬 개발 DB에만 적용합니다.')
    environment = os.environ.copy()
    environment['MYSQL_PWD'] = setting('password')
    command = ['mariadb', '--skip-ssl', '--default-character-set=utf8mb4', '--batch', '--raw', '--skip-column-names',
               '--host='+url[1], '--port='+(url[2] or '3306'), '--user='+setting('username'), url[3]]
    def execute(sql):
        result = subprocess.run(command, input=sql, env=environment, text=True, encoding='utf-8', capture_output=True)
        if result.returncode:
            raise RuntimeError(result.stderr)
        return result.stdout
    def rows(sql):
        return [json.loads(line) for line in execute(sql).splitlines() if line]
    owners = rows("SELECT JSON_OBJECT('id',member_id) FROM members WHERE login_id='user1' AND status='ACTIVE' AND deleted_at IS NULL;")
    if len(owners) != 1:
        raise RuntimeError('활성 user1 시험 계정을 찾을 수 없습니다.')
    owner = int(owners[0]['id'])
    activities = rows(f"""
        SELECT JSON_OBJECT('id',a.id,'courseId',c.id,'day',d.day_no,'seq',a.seq,'region',c.destination,
            'placeId',a.place_id,'name',a.place_name,'title',a.title,'address',a.address_snapshot,
            'latitude',a.latitude_snapshot,'longitude',a.longitude_snapshot)
        FROM activity a JOIN course_day d ON d.id=a.course_day_id JOIN course c ON c.id=d.course_id
        WHERE c.member_no={owner} AND c.deleted_at IS NULL AND c.is_active=TRUE
          AND JSON_UNQUOTE(JSON_EXTRACT(c.condition_meta,'$.fixture')) LIKE 'MYPAGE-USER1-%'
        ORDER BY c.id,d.day_no,a.seq,a.id;
        """)
    drafts = rows(f"""
        SELECT JSON_OBJECT('id',id,'region',region,'days',JSON_QUERY(days_json,'$'))
        FROM mypage_course_draft WHERE member_id={owner} AND client_id LIKE 'mypage-user1-saved-%' ORDER BY id;
        """)
    regions = sorted({item['region'] for item in activities+drafts})
    candidates = {}
    for region in regions:
        value = literal(region)
        candidates[region] = rows(f"""
            SELECT JSON_OBJECT('placeId',p.place_id,'name',p.name,'address',p.address,
                'latitude',p.latitude,'longitude',p.longitude,
                'photo',CASE WHEN EXISTS(SELECT 1 FROM place_image i WHERE i.place_id=p.place_id AND NULLIF(TRIM(i.image_url),'') IS NOT NULL) THEN 1 ELSE 0 END,
                'description',CASE WHEN NULLIF(TRIM(p.description),'') IS NOT NULL THEN 1 ELSE 0 END)
            FROM place p LEFT JOIN region area ON area.region_id=p.region_id
            WHERE p.is_active=TRUE AND p.source_provider<>'TEST'
              AND p.latitude BETWEEN -90 AND 90 AND p.longitude BETWEEN -180 AND 180
              AND p.latitude<>0 AND p.longitude<>0
              AND (LOCATE({value},p.address)=1 OR LOCATE(CONCAT(' ',{value},'시 '),p.address)>0
                OR LOCATE(CONCAT(' ',{value},'군 '),p.address)>0 OR LOCATE(CONCAT(' ',{value},' '),p.address)>0
                OR area.region_name={value} OR area.sigungu_name={value} OR LOCATE({value},area.sido_name)=1
                OR ({value} IN ('광주','광주광역시') AND p.address REGEXP '^전남광주통합특별시 (동구|서구|남구|북구|광산구) '))
              AND ({value} NOT IN ('광주','광주광역시') OR LOCATE('광주광역시',p.address)=1
                OR p.address REGEXP '^전남광주통합특별시 (동구|서구|남구|북구|광산구) ')
            ORDER BY (EXISTS(SELECT 1 FROM place_image i WHERE i.place_id=p.place_id AND NULLIF(TRIM(i.image_url),'') IS NOT NULL)
                AND NULLIF(TRIM(p.description),'') IS NOT NULL) DESC,
                EXISTS(SELECT 1 FROM place_image i WHERE i.place_id=p.place_id AND NULLIF(TRIM(i.image_url),'') IS NOT NULL) DESC,
                CASE p.place_type WHEN 'ATTRACTION' THEN 0 WHEN 'CULTURE' THEN 1 WHEN 'LEISURE' THEN 2 ELSE 3 END,
                (NULLIF(TRIM(p.description),'') IS NOT NULL) DESC,p.place_id;
            """)
        # 같은 장소가 다른 공급자 ID로 중복 적재되었어도 일정에 중복 배정하지 않는다.
        seen = set(); unique = []
        for place in candidates[region]:
            key = (re.sub(r'\s+', '', place['name']), re.sub(r'\s+', '', place['address'] or ''))
            if key not in seen:
                unique.append(place); seen.add(key)
        candidates[region] = unique
        if not unique:
            raise RuntimeError('좌표가 있는 DB 장소가 없어 적용하지 않습니다: '+region)
    allocation = {}
    def take(region, course_key):
        index = allocation.get(course_key, 0)
        places = candidates[region]
        if index >= len(places):
            raise RuntimeError('일정에 필요한 지역별 장소 수가 부족합니다: '+region)
        allocation[course_key] = index+1
        return places[index]
    updates = []; rollback = []; replacements = []
    for activity in activities:
        place = take(activity['region'], ('course', activity['courseId']))
        updates.append(f"""UPDATE activity a JOIN course_day d ON d.id=a.course_day_id JOIN course c ON c.id=d.course_id
            SET a.place_id={int(place['placeId'])},a.place_name={literal(place['name'])},a.title={literal(place['name'])},
                a.address_snapshot={literal(place['address'])},a.latitude_snapshot={float(place['latitude'])},a.longitude_snapshot={float(place['longitude'])}
            WHERE a.id={int(activity['id'])} AND c.member_no={owner}
              AND JSON_UNQUOTE(JSON_EXTRACT(c.condition_meta,'$.fixture')) LIKE 'MYPAGE-USER1-%';""")
        rollback.append(f"UPDATE activity SET place_id={activity['placeId'] or 'NULL'},place_name={literal(activity['name'])},title={literal(activity['title'])},address_snapshot={literal(activity['address'])},latitude_snapshot={activity['latitude'] if activity['latitude'] is not None else 'NULL'},longitude_snapshot={activity['longitude'] if activity['longitude'] is not None else 'NULL'} WHERE id={int(activity['id'])};")
        replacements.append({'region':activity['region'],**place})
    for draft in drafts:
        days = draft['days']
        if isinstance(days,str): days=json.loads(days)
        # 복구할 원래 일정은 별도 객체로 보존한다.
        new_days = json.loads(json.dumps(days))
        for day in new_days:
            count = len(day.get('places') or day.get('mapPlaces') or [])
            places = [take(draft['region'], ('draft',draft['id'])) for _ in range(count)]
            day['places'] = [place['name'] for place in places]
            day['mapPlaces'] = [{key:place[key] for key in ['placeId','name','address','latitude','longitude']} for place in places]
            replacements.extend({'region':draft['region'],**place} for place in places)
        updates.append(f"UPDATE mypage_course_draft SET days_json={literal(json.dumps(new_days,ensure_ascii=False))} WHERE id={int(draft['id'])} AND member_id={owner} AND client_id LIKE 'mypage-user1-saved-%';")
        rollback.append(f"UPDATE mypage_course_draft SET days_json={literal(json.dumps(days,ensure_ascii=False))} WHERE id={int(draft['id'])} AND member_id={owner};")
    summary = {'apply': options.apply, 'courses':len({item['courseId'] for item in activities}),
               'drafts':len(drafts),'places':len(replacements),
               'photos':sum(bool(item['photo']) for item in replacements),
               'descriptions':sum(bool(item['description']) for item in replacements),
               'regions':{region:len(candidates[region]) for region in regions},
               'selected':{region:[{'name':p['name'],'address':p['address']} for p in candidates[region][:2]] for region in regions}}
    if options.apply:
        stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
        backup_dir = ROOT/'logs'/'mypage-place-backups'; backup_dir.mkdir(parents=True,exist_ok=True)
        (backup_dir/(stamp+'.json')).write_text(json.dumps({'activities':activities,'drafts':drafts},ensure_ascii=False,indent=2),encoding='utf-8')
        (backup_dir/(stamp+'-restore.sql')).write_text('SET NAMES utf8mb4;\nSTART TRANSACTION;\n'+'\n'.join(rollback)+'\nCOMMIT;\n',encoding='utf-8')
        execute('SET NAMES utf8mb4;\nSTART TRANSACTION;\n'+'\n'.join(updates)+'\nCOMMIT;')
        summary['backup']=str(backup_dir/(stamp+'.json'))
    print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':
    main()
