# SkinAI Doctor API V2 Draft

## Design goals
- Doctor-facing workflow first.
- Case-centric data model: patient + case + analysis + follow-up.
- Keep AI chat as independent domain.
- Announcement is placeholder only: always return "暂无此功能".

## Response convention
- `code=0` success, `code=1` failure
- body shape: `{ code, msg, result }`

## Auth
- `POST /api/login/pwd`
- `POST /api/login/wxMin` (legacy/optional)

---
## 1) Doctor profile

### GET `/api/doctor/me`
- result:
```json
{
  "id": 1,
  "username": "doctor01",
  "nickname": "张医生",
  "mobile": "13900000001",
  "jobNumber": "DOC0001",
  "department": "皮肤科",
  "title": "主治医师",
  "status": "ONLINE"
}
```

### PUT `/api/doctor/me`
- req:
```json
{
  "nickname": "张医生",
  "department": "皮肤科",
  "title": "主治医师",
  "status": "ONLINE"
}
```

---
## 2) Dashboard

### GET `/api/dashboard/summary`
- result:
```json
{
  "todayDiagnosed": 16,
  "pendingCases": 8,
  "followupCases": 5,
  "historyCases": 122
}
```

### GET `/api/dashboard/announcements`
- temporary behavior:
```json
{ "code": 1, "msg": "暂无此功能", "result": null }
```

---
## 3) Patient

### POST `/api/patients`
- req:
```json
{
  "patientName": "李四",
  "gender": "男",
  "age": 44,
  "phone": "13800000000",
  "notes": "初诊"
}
```

### GET `/api/patients`
- query: `keyword`, `page`, `pageSize`

### GET `/api/patients/{id}`

### PUT `/api/patients/{id}`

---
## 4) Medical case

### POST `/api/cases`
- req:
```json
{
  "patientId": 1001,
  "chiefComplaint": "局部红斑",
  "presentHistory": "约2周",
  "treatmentHistory": "未系统治疗",
  "duration": "2周",
  "extraNotes": "轻度瘙痒"
}
```

### GET `/api/cases`
- query:
  - `status`: `PENDING|IN_PROGRESS|FOLLOWUP|DONE|CLOSED`
  - `patientId`
  - `keyword`
  - `page`, `pageSize`

### GET `/api/cases/{id}`

### PUT `/api/cases/{id}`

### PUT `/api/cases/{id}/status`
- req:
```json
{ "status": "FOLLOWUP" }
```

---
## 5) Case image and analysis

### POST `/api/cases/{id}/images:upload`
- multipart upload, return temporary object key / preview url

### POST `/api/cases/{id}/analyze`
- behavior:
  - call python model service
  - persist `case_analysis` and `case_concept_score`
- result:
```json
{
  "diseaseType": "lichen_planus",
  "confidence": 0.8733,
  "conceptScores": [
    { "conceptIndex": 56, "conceptNameCn": "白色条纹", "score": 0.3282, "rankNo": 1 }
  ]
}
```

### GET `/api/cases/{id}/analysis`

---
## 6) Follow-up

### POST `/api/cases/{id}/followups`
- req:
```json
{
  "followupTime": "2026-05-10T10:00:00",
  "summary": "症状减轻",
  "nextPlan": "两周后复查"
}
```

### GET `/api/cases/{id}/followups`

---
## 7) AI chat (keep existing)
- `POST /api/deepseek/chat`
- `GET /api/deepseek/chat`
- `GET /api/deepseek/chat/{chatId}`
- `DELETE /api/deepseek/chat/{chatId}`

---
## Front-end route mapping proposal
- Tab `工作台` -> `/pages/workbench/index`
- Tab `开始诊断` -> `/pages/diagnosis/create`
- Tab `诊疗记录` -> `/pages/case/list`
- Tab `AI问诊` -> `/pages/chat/chat`

`我的` moved to workbench top-right entry: `/pages/me/profile`

---
## Implementation order (recommended)
1. DB create new tables (`doctor_schema_v2.sql`)
2. Backend: patient + case CRUD + dashboard summary
3. Backend: case analyze + concept score persistence
4. Front-end: switch tab + case workflow pages
5. Retire family/report legacy APIs after migration
