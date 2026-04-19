# API è§„æ ¼è‰æ¡ˆ

## 1. ç›®æ ‡

å½“å‰ä¸º V1.1 å ä½è§„æ ¼ï¼ŒæœåŠ¡äºåŒç«¯é—­ç¯ã€æé†’è®¡åˆ’åŒæ­¥ã€ä»Šæ—¥ç»“è®ºå’Œå¼‚å¸¸å›ä¼ ã€‚

## 2. æ ¸å¿ƒå¯¹è±¡

### `user_profile`
- `preferred_name`
- `likes`
- `dislikes`
- `taboo_words`
- `common_topics`

### `family_binding`
- `senior_id`
- `child_id`
- `relation`
- `notification_pref`
- `binding_code`
- `binding_method`
- `validation_status`
- `bound_at`

### `care_plan`
- `type`
- `schedule`
- `frequency`
- `priority`
- `confirm_required`

### `care_plan_draft`
- `source_text`
- `parsed_fields`
- `needs_confirmation`

### `event_log`
- `event_type`
- `payload`
- `source`
- `confidence`
- `created_at`

### `daily_summary`
- `date`
- `summary_text`
- `risk_level`
- `todo_left`

### `memory_fact`
- `key`
- `value`
- `source`
- `verified`

## 3. å»ºè®®æ¥å£

### 3.1 ç»‘å®š
- `POST /api/bindings`
  - ç”¨é€”ï¼šè¾“å…¥ç»‘å®šç å®Œæˆå®¶åº­ç»‘å®š
- `POST /api/bindings/scan-preview`
  - ç”¨é€”ï¼šæ‰«ç åå…ˆè¿”å›ç¤ºä¾‹ç»‘å®šç»“æœæˆ–é¢„æ ¡éªŒç»“æœï¼Œæ­£å¼æ¥æœåŠ¡ç«¯å‰å¯ä½œä¸º H5 çš„å ä½æ¥å£è¾¹ç•Œ
- `GET /api/bindings/{bindingId}`
  - ç”¨é€”ï¼šè¯»å–å½“å‰ç»‘å®šå…³ç³»ã€ç»‘å®šæ–¹å¼å’Œæ ¡éªŒçŠ¶æ€

### 3.2 è€äººèµ„æ–™
- `GET /api/seniors/{seniorId}/profile`
- `PUT /api/seniors/{seniorId}/profile`

### 3.3 æé†’è®¡åˆ’
- `GET /api/seniors/{seniorId}/care-plans`
- `POST /api/seniors/{seniorId}/care-plans`
- `PUT /api/care-plans/{planId}`
- `POST /api/care-plan-drafts`
- `POST /api/care-plan-drafts/{draftId}/confirm`

### 3.4 ä»Šæ—¥ç»“è®ºä¸å¼‚å¸¸
- `GET /api/seniors/{seniorId}/daily-summary`
- `GET /api/seniors/{seniorId}/anomalies`

### 3.5 äº‹ä»¶ä¸è®°å¿†
- `POST /api/seniors/{seniorId}/events`
- `GET /api/seniors/{seniorId}/memory-facts`

## 4. é£æ§è¦æ±‚

- è¯åã€å‰‚é‡ã€æ—¥æœŸã€è”ç³»äººã€å¤è¯Šä¿¡æ¯ã€å¼‚å¸¸äº‹ä»¶ç­‰é«˜é£é™©å­—æ®µï¼Œæœªç¡®è®¤å‰åªèƒ½å­˜åœ¨è‰ç¨¿æ€
- å¼‚å¸¸ä¸ŠæŠ¥å¿…é¡»å…ˆèµ°è§„åˆ™å’Œç²—ç­›ï¼Œé¿å…è¯¯æŠ¥
- æ¨æ–­å±‚ä¿¡æ¯ä¸èƒ½ç›´æ¥ä½œä¸ºæ­£å¼ç…§æŠ¤åŠ¨ä½œä¾æ®
## 2026-04-13 ??????????
### ??/??????
- senior_profile
  - senior_id
  - preferred_name
  - elation_label
  - interests
  - hobbies
  - 	aboo_topics
  - communication_style
  - outine_summary
  - persona_tags
  - important_contacts
  - updated_at
- care_plan
  - plan_id
  - senior_id
  - 	itle
  - schedule
  - requency
  - channel
  - confirm_required
  - source
  - status
  - updated_at
- care_plan_event
  - event_id
  - plan_id
  - event_type
  - payload
  - created_at
- 	opic_brief
  - 	opic_id
  - senior_id
  - 	itle
  - summary
  - source_name
  - source_url
  - isk_flags
  - generated_at
### ??????
- GET /api/health
  - ???????????
- GET /api/seniors/{seniorId}
  - ??????????????
- PUT /api/seniors/{seniorId}
  - ??????????????????????????
- GET /api/seniors/{seniorId}/care-plans
  - ???????????/?????
- POST /api/seniors/{seniorId}/care-plans
  - ????????????
- PUT /api/care-plans/{planId}
  - ????????????
- POST /api/care-plans/{planId}/events
  - ????????????????????????
- GET /api/seniors/{seniorId}/topic-briefs
  - ???????????????????????????????????????????
- GET /api/seniors/{seniorId}/sync-packet
  - ???????/??????????????????????????????????
- GET /api/seniors/{seniorId}/care-plan-events
  - ????????????????????????????????????
### 3.6 é™ªä¼´å›å¤ä¸è¯­ä¹‰è®°å¿†
- `POST /api/ai/companion-reply`
  - è¯·æ±‚ä¸­çš„ `conversationContext` æ–°å¢ `personaPrompt` å’Œ `collectionHint`ï¼Œç”¨äºä¸‹å‘åŒè§’è‰²ç³»ç»Ÿæç¤ºè¯å’Œä¸»åŠ¨ç”»åƒæ”¶é›†æç¤ºã€‚
  - è¯·æ±‚ä¸­çš„ `semanticMemories` æ–°å¢ `Profile`ã€`Experience` ç­‰ç±»å‹ï¼Œç”¨äºæ‰¿æ¥è€äººçš„åŸºæœ¬æƒ…å†µä¸äººç”Ÿç»å†ã€‚
- `PUT /api/seniors/{seniorId}/semantic-memories`
  - ä¿å­˜è€äººç«¯åŒæ­¥è¿‡æ¥çš„é•¿æœŸè¯­ä¹‰è®°å¿†ï¼Œæ”¯æŒ `Profile`ã€`Family`ã€`Experience` ç­‰åˆ†ç±»ã€‚

## 2026-04-18 Åã°é»Ø¸´Óë¶à²ã¼ÇÒä½Ó¿Ú²¹³ä

### `POST /api/ai/companion-reply`
- ·µ»ØÌåÖĞµÄ `mode` ¼ÌĞøÊ¹ÓÃ `live | fallback`£¬µ«Á´Â·ÊµÏÖÉÏÓ¦±£Ö¤£º
  - Ö»ÓĞ¼«¶ÌÈ·ÈÏÏûÏ¢ÔÊĞíÔÚ¿Í»§¶ËÖ±½Ó±¾µØ·µ»Ø£¬²»¾­¹ı·şÎñ¶Ë¡£
  - ÆÕÍ¨ÏĞÁÄ¡¢ÌìÆø¡¢ÈÕ³£×´Ì¬µÈÏûÏ¢Ä¬ÈÏ½»ÓÉ·şÎñ¶Ë¾ö¶¨ÊÇ·ñ live¡£
- `usedContext.memoryHighlights` Ö»ÔÊĞíĞ¯´ø¸ßÏà¹Ø¡¢¿É¸´ÓÃµÄ¼ÇÒäÕªÒª£¬±ÜÃâ°ÑÌìÆøÆÀ¼Û¡¢ÎÊ¾äºÍ·º»¯ÇéĞ÷Æ´Èë prompt¡£

### `PUT /api/seniors/{seniorId}/semantic-memories`
- µ¥Ìõ¼ÇÒäĞÂÔöÍÆ¼ö×Ö¶Î£º
  - `memoryLayer`: `profile | preference | recent_state`
  - `retention`: `long_term | short_term`
  - `evidenceCount`: number
  - `lastConfirmedAt`: number
  - `expiresAt`: number | null
- ×Ö¶ÎÓïÒå£º
  - `memoryLayer` ÓÃÓÚÉèÖÃÒ³·Ö×éÓëÕÙ»Ø°×Ãûµ¥¡£
  - `retention` ÓÃÓÚÇø·Ö³¤ÆÚ±£ÁôºÍ¿É¹ıÆÚµÄ½üÆÚ×´Ì¬¡£
  - `evidenceCount` ÓÃÓÚºâÁ¿¼ÇÒäÊÇ·ñ±»¶à´ÎÌá¼°£¬²»ÔÙÖ»ÒÀÀµ `sourceCount`¡£
  - `lastConfirmedAt` ±íÊ¾×î½üÒ»´Î±»ÔÙ´ÎÃüÖĞ»òÈ·ÈÏµÄÊ±¼ä¡£
  - `expiresAt` ÓÃÓÚ¶ÌÆÚ×´Ì¬×Ô¶¯Ë¥¼õ£¬¹ıÆÚºó²»ÔÙ²ÎÓëÄ¬ÈÏÕÙ»Ø¡£
- ¹ıÂË¹æÔò£º
  - ÎÊ¾ä¡¢ÌìÆøÆÀ¼Û¡¢¶Ìº®êÑ¡¢Ò»´ÎĞÔÇáÇéĞ÷Ä¬ÈÏ²»Ó¦Ğ´Èë³¤ÆÚ¼ÇÒä¡£
  - ÉíÌå²»ÊÊºÍÇéĞ÷ÀàÄÚÈİÖ»ÓĞÔÚÖØ¸´³öÏÖ¡¢´øÎÈ¶¨ÌØÕ÷»òÃ÷È·ÊôÓÚ³¤ÆÚ»ùÏßÊ±£¬²ÅÔÊĞí³Ö¾Ã»¯Îª³¤ÆÚ¼ÇÒä¡£
