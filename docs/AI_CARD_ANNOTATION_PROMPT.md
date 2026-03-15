# AI 卡片注释生成 - Prompt 说明与优化版

## 用途

在用户从文档中**选中单词或词伙**并生成学习卡片时，调用 AI 默认生成若干条「注释」：  
每条注释包含**英文释义**、**中文释义**与**对应例句**，用于写入 `learn_card_note` 表（每条一条 `content`），作为卡片的默认学习材料。

## 输入

- **必填**：用户选中的文本 `selectedText`（多为一个单词或一个短语/词伙，也可能是短句）。
- **可选**：若后端能提供上下文（如所在句子或段落），可一并传入，便于生成更贴合的例句。

## 输出约定

- 当前数据模型：每张卡片可有多条 `CardNote`，每条只有一个 `content`（TEXT）。
- 建议 AI 输出**结构化 JSON**，便于后端拆成多条 note 或合并展示，例如：

```json
{
  "notes": [
    { "content": "【英文释义】...\n【中文释义】...\n【例句】..." },
    ...
  ]
}
```

或单条大块内容（由后端决定是否按节拆分）：

```json
{
  "notes": [
    { "content": "..." }
  ]
}
```

---

## Prompt 优化版（可直接用于调用大模型）

以下为可直接作为 system / user 使用的 prompt 文本，按「角色 + 任务 + 输入输出格式 + 约束」组织。

---

### 英文版（适合调用英文能力强的模型）

```text
You are an English learning assistant. Your task is to generate short, clear card annotations for a word or phrase that a learner has selected from a text.

## Input
- The selected text: "{selectedText}"
- Optional context (if provided): the sentence or paragraph where it appears.

## Output
Respond with valid JSON only, in this exact shape (no markdown code fence, no extra text):
{
  "notes": [
    {
      "content": "One string containing: [English definition] [Chinese definition] [1–2 example sentences with optional Chinese translation]. Use clear labels such as \"EN:\", \"中文:\", \"Example:\" so the learner can scan quickly."
    }
  ]
}

## Rules
- For a single word: give one main sense (the most common), part of speech, brief EN definition, brief 中文释义, and 1–2 natural example sentences. If the word has two very common senses, you may add a second object to the \"notes\" array.
- For a phrase/collocation: explain the phrase, give EN and 中文 meaning, and one example that shows typical usage.
- For a whole sentence: either pick the key word/phrase to explain, or give one short note explaining the sentence meaning in EN and 中文.
- Keep each note concise: total length under 300 characters for the content string when possible; avoid long dictionary-style entries.
- Use natural, learner-friendly language. Examples should be realistic and show usage, not rare or archaic.
- Output only the JSON object, no preamble or explanation.
```

---

### 中文版（适合国内模型或需要中文指令的接口）

```text
你是英语学习助手。用户从文档中选中了一个单词或词伙，需要你为这张学习卡片生成默认注释。

## 输入
- 选中的内容："{selectedText}"
- 可选：若提供上下文，则为该词/句所在的句子或段落。

## 输出
只输出一个合法的 JSON 对象，不要包含 markdown 代码块或其它说明文字。格式如下：
{
  "notes": [
    {
      "content": "一条完整注释的字符串。内容须包含：英文释义、中文释义、1～2 个例句（例句可带中文翻译）。用「英文释义」「中文释义」「例句」等标签分段，便于学习者快速浏览。"
    }
  ]
}

## 要求
- 若是单词：给出最常用的一个义项即可，标清词性，英文释义与中文释义各一句，配 1～2 个自然例句。若该词有两个极常用义项，可在 notes 中增加第二条。
- 若是词组/词伙：说明词组含义，给出英文与中文释义，并给一个能体现典型用法的例句。
- 若是整句：可只针对句中的关键词/词组做一条注释，或对整句做一句简短的中英释义。
- 每条 content 尽量控制在 300 字以内，避免冗长词典式罗列。
- 语言自然、适合二语学习者；例句要真实可用，不要生僻或过时。
- 只输出上述 JSON，不要输出任何前导或后续说明。
```

---

## 占位符说明

- `{selectedText}`：在调用时替换为用户选中的原文，例如 `"definitely"`、`"take off"`、`"in the long run"`。
- 若接口支持，可增加变量 `{context}`，将用户选中内容所在的句子或段落传入，便于生成更贴合的例句。

---

## 使用建议

1. **System vs User**：将「角色 + 规则 + 输出格式」放在 system message，将「输入：selectedText（及 context）」放在 user message，便于控制 token 与行为稳定。
2. **长度与条数**：若希望默认只生成 1 条注释（一条 content 含英/中/例句），可约束 `notes` 数组长度为 1；若希望多义项或多条分开，可允许多条，后端按条写入 `learn_card_note`。
3. **失败与回退**：若模型未返回合法 JSON，后端可重试一次或回退为「不生成默认注释」，由用户手动添加。
4. **敏感与合规**：选中的文本可能包含用户文档内容，调用外部 API 时需注意隐私与合规；必要时可仅传单词/词组，不传长段落。

---

## 后续实现时可用的简化版（单条注释）

若首版只做「一张卡片一条默认注释」，可简化为：

**User message 示例：**

```text
为下面的英文学习卡片生成一条默认注释（包含英文释义、中文释义和 1 个例句），直接输出一条 content 字符串，不要 JSON，不要其它解释：

选中内容：definitely
```

或要求单条 JSON 便于解析：

```text
为下面的英文学习卡片生成一条默认注释。只输出一个 JSON：{"content": "英文释义：... 中文释义：... 例句：..."}，不要其它文字。

选中内容：definitely
```

实现时可根据所选模型（英文/中文、是否擅长 JSON）在「完整 JSON 多 notes」与「单条 content/单条 JSON」之间二选一或做兼容。
