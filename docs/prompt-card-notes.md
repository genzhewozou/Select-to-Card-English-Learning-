# 生成卡片注释 — AI Prompt 说明

用于在用户从文档中选中单词或词伙并生成卡片时，自动生成一条「注释」，包含英文释义、中文释义与例句，便于学习与复习。

---

## 一、角色与任务

**System / 角色（可选，用于多轮或带身份的模型）：**

```
You are an English learning assistant. You generate concise, accurate vocabulary notes for flashcards: English definition, Chinese definition, and 1–2 natural example sentences. Output in a fixed structure so it can be stored as a single note.
```

**User / 主任务说明（必用）：**

```
Generate a vocabulary note for the following word or phrase from an English learning context.

**Target:** {selected_text}

**Optional context (sentence where it appeared):** {context_sentence}

Produce exactly one note with these three parts, in this order. Use the labels as given so the app can parse or display them clearly.

1. **English definition**  
   One short, clear definition in English (phrase or one sentence). Prefer common meaning; if the word has multiple senses, give the most likely meaning for general learning.

2. **中文释义**  
   One concise Chinese explanation (对应上面英文释义的含义).

3. **Example(s)**  
   1–2 natural example sentences in English that use the word/phrase. You may add a short Chinese translation in parentheses after each sentence if helpful.

Format the note in plain text or Markdown. Keep the note compact: total length under 300 words. Do not invent rare or obscure meanings; if you are unsure, give the most common meaning only.
```

---

## 二、占位符说明

| 占位符 | 含义 | 是否必填 |
|--------|------|----------|
| `{selected_text}` | 用户选中的单词或词伙，作为卡片正面 | 必填 |
| `{context_sentence}` | 选中内容所在的完整句子（可选），帮助消歧与生成更贴切的例句 | 选填，无则留空或省略该行 |

---

## 三、输出格式约定（便于存储与展示）

当前卡片注释表 `learn_card_note` 的 `content` 为单字段 TEXT，建议 AI 输出**一条**结构化内容，例如：

```text
**English definition**  
(to do something) with certainty; without doubt.

**中文释义**  
肯定地；确定地。

**Example(s)**  
• She will definitely be there. （她肯定会来的。）
• I definitely want to go. （我确实想去。）
```

或使用更紧凑的 Markdown（前端可按 Markdown 渲染）：

```markdown
**English definition**  
with certainty; without doubt.

**中文释义**  
肯定地；确定地。

**Example(s)**  
- She will definitely be there.（她肯定会来。）
- I definitely want to go.（我确实想去。）
```

这样一条字符串可直接写入 `CardNote.content`，前端用同一套展示逻辑即可。

---

## 四、约束与质量要求（可写进 prompt 或 system）

- **长度**：整条注释控制在 300 词以内，释义各 1 句为主，例句 1–2 句。
- **准确性**：只给常见、可查证的含义；不确定时只给最常用义项。
- **例句**：句子需自然、语法正确，且必须包含目标词/词伙；中文翻译可选、简短。
- **语言**：英文释义与例句为英文，中文释义与例句翻译为中文。
- **禁止**：不编造词义、不生成与目标词无关的内容、不输出冗长段落。

---

## 五、精简版单段 Prompt（便于直接拼进代码）

若调用时只传一段 user message，可用下面这一整段（将占位符替换为实际值）：

```
Generate a vocabulary note for this word/phrase. Output exactly three parts in order: (1) "English definition" — one short definition in English; (2) "中文释义" — one concise Chinese explanation; (3) "Example(s)" — 1–2 natural English sentences using the word/phrase, with optional Chinese in parentheses. Use the labels above. Keep the whole note under 300 words. Target: {selected_text}. Context (optional): {context_sentence}
```

---

## 六、后续实现时可考虑的扩展

- **多义项**：若需支持多义项，可约定输出多个 `**Sense 1**` / `**Sense 2**` 块，或拆成多条 `CardNote`。
- **词性**：若需要，可在「English definition」前增加一行 `**Part of speech**`（e.g. adverb, phrasal verb）。
- **流式**：若使用流式 API，可约定先输出固定小标题再流式输出正文，便于前端分段展示。

以上为纯 prompt 设计，不涉及具体接口或代码实现；实现时只需将 `{selected_text}` 与 `{context_sentence}` 替换为实际值并调用所选大模型 API 即可。
